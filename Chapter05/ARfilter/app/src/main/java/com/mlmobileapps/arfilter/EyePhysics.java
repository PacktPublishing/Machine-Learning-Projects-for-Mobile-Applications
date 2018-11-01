/*
 * Copyright (c) 2017 Razeware LLC
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * Notwithstanding the foregoing, you may not use, copy, modify, merge, publish, 
 * distribute, sublicense, create a derivative work, and/or sell copies of the 
 * Software in any work that is designed, intended, or marketed for pedagogical or 
 * instructional purposes related to programming, coding, application development, 
 * or information technology.  Permission for such use, copying, modification,
 * merger, publication, distribution, sublicensing, creation of derivative works, 
 * or sale is expressly withheld.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.mlmobileapps.arfilter;

import android.graphics.PointF;
import android.os.SystemClock;

/**
 * Simulates the physics of motion for an iris which moves within a googly eye.  The iris moves
 * independently of the motion of the face/eye, and moves according to the following forces:<p>
 *
 * <ol>
 * <li>Gravity - downward acceleration.</li>
 *
 * <li>Friction - deceleration; opposing motion</li>
 *
 * <li>Bounce - acceleration in the opposite direction of motion when the iris hits the side of the
 * eye (e.g., due to a jerking motion which suddenly moves the face in frame).  Note that this is
 * the only way to get the iris to move horizontally, since gravity only accelerates downward.</li>
 * </ol>
 *
 * The simulation is configured to run at a universal real time rate, regardless of the performance
 * of the device in which it is run and how frequently updates are received.
 */
class EyePhysics {

  private static final String TAG = "EyePhysics";

  // The friction and gravity values below are set relative to a specific time period.  This
  // allows the simulation to run at the same rate, regardless of whether it is running on a slow
  // or fast device or if there are temporary performance variations on the device.
  private final long TIME_PERIOD_MS = 1000;

  private final float FRICTION = 2.2f;
  private final float GRAVITY = 40.0f;

  private final float BOUNCE_MULTIPLIER = 20.0f;

  // Allow slightly non-zero values to be considered to be zero, to converge to zero more quickly.
  private final float ZERO_TOLERANCE = 0.001f;

  private long mLastUpdateTimeMs = SystemClock.elapsedRealtime();

  private PointF mEyePosition;
  private float mEyeRadius;

  private PointF mIrisPosition;
  private float mIrisRadius;

  // Velocity is independent of the final rendering coordinate system, so that we don't have to
  // change it as the eye gets bigger or smaller by forward and backward motion.  This will be
  // scaled up proportional to the eye size when updating position.
  private float vx = 0.0f;
  private float vy = 0.0f;

  // Keep track of bounces that immediately occur consecutively, since this means that the
  // iris is bouncing too fast.  When this happens, we dampen the velocity to avoid infinite
  // bounces.
  private int mConsecutiveBounces = 0;

  //==============================================================================================
  // Methods
  //==============================================================================================

  /**
   * Generate the next position of the iris based on simulated velocity, eye boundaries, gravity,
   * friction, and bounce momentum.
   */
  PointF nextIrisPosition(PointF eyePosition, float eyeRadius, float irisRadius) {
    // Correct the current eye position and size based on recent motion of the face within the
    // frame.  Keep the current iris position, if available.
    mEyePosition = eyePosition;
    mEyeRadius = eyeRadius;
    if (mIrisPosition == null) {
      mIrisPosition = eyePosition;
    }
    mIrisRadius = irisRadius;

    // Keep track of time, so that we can consistently update the simulation proportionally to
    // how much time has elapsed.  This makes the animation rate device-independent.  All of the
    // velocity changes below are pro-rated based on this.
    long nowMs = SystemClock.elapsedRealtime();
    long elapsedTimeMs = nowMs - mLastUpdateTimeMs;
    float simulationRate = (float) elapsedTimeMs / TIME_PERIOD_MS;
    mLastUpdateTimeMs = nowMs;

    if (!isStopped()) {
      // Only apply gravity when the iris is not stopped at the bottom of the eye.
      vy += GRAVITY * simulationRate;
    }

    // Apply friction in the opposite direction of motion, so that the iris slows in the absence
    // of other head motion.
    vx = applyFriction(vx, simulationRate);
    vy = applyFriction(vy, simulationRate);

    // Update the iris position based on velocity.  Since velocity is size-independent, scale by
    // the iris radius to get the change in position.
    float x = mIrisPosition.x + (vx * mIrisRadius * simulationRate);
    float y = mIrisPosition.y + (vy * mIrisRadius * simulationRate);
    mIrisPosition = new PointF(x, y);

    // Correct the position and velocity of the iris if it has gone out of bounds, guaranteeing
    // that the returned result is at a valid position within the eye.
    makeIrisInBounds(simulationRate);

    return mIrisPosition;
  }

  /**
   * Friction slows velocity in the opposite direction of motion, until zero velocity is reached.
   */
  private float applyFriction(float velocity, float simulationRate) {
    if (isZero(velocity)) {
      velocity = 0.0f;
    } else if (velocity > 0) {
      velocity = Math.max(0.0f, velocity - (FRICTION * simulationRate));
    } else {
      velocity = Math.min(0.0f, velocity + (FRICTION * simulationRate));
    }
    return velocity;
  }

  /**
   * Correct the iris position to be in-bounds within the eye, if it is now out of bounds.  Being
   * out of bounds could have been due to a sudden movement of the head and/or camera, or the
   * result of just bouncing/rolling around.<p>
   *
   * In addition, modify the velocity to cause a bounce in the opposite direction.
   */
  private void makeIrisInBounds(float simulationRate) {
    float irisOffsetX = mIrisPosition.x - mEyePosition.x;
    float irisOffsetY = mIrisPosition.y - mEyePosition.y;

    float maxDistance = mEyeRadius - mIrisRadius;
    float distance = (float) Math.sqrt(Math.pow(irisOffsetX, 2) + Math.pow(irisOffsetY, 2));
    if (distance <= maxDistance) {
      // The iris is in bounds, so no correction is necessary.
      mConsecutiveBounces = 0;
      return;
    }

    // Accumulate a consecutive bounce count, in order to dampen the momentum of a quickly
    // moving iris.  Two or more bounces in a row indicates that the iris is moving so fast that
    // it doesn't even travel inside the eye.  We progressively slow the velocity using this
    // count until this is no longer the case.
    mConsecutiveBounces++;

    // Move the iris back to where it would have been when it would have contacted the side of
    // the eye.
    float ratio = maxDistance / distance;
    float x = mEyePosition.x + (ratio * irisOffsetX);
    float y = mEyePosition.y + (ratio * irisOffsetY);

    // Update the velocity direction and magnitude to cause a bounce.

    float dx = x - mIrisPosition.x;
    vx = applyBounce(vx, dx, simulationRate) / mConsecutiveBounces;

    float dy = y - mIrisPosition.y;
    vy = applyBounce(vy, dy, simulationRate) / mConsecutiveBounces;

    mIrisPosition = new PointF(x, y);
  }

  /**
   * Update velocity in response to bouncing off the sides of the eye (i.e., when iris hits the
   * bottom or the eye moves quickly).  This is the only way to gain horizontal velocity, since
   * there is no other horizontal force.
   */
  private float applyBounce(float velocity, float distOutOfBounds, float simulationRate) {
    if (isZero(distOutOfBounds)) {
      // No bounce needed, since we are still in bounds along this dimension.
      return velocity;
    }

    // Reverse velocity to create a bounce in the opposite direction.
    velocity *= -1;

    // If distOutOfBounds was large, this indicates that the iris was whacked against the side
    // of the eye quickly.  Add an additional velocity factor to account for the force gained by
    // this quick movement, based upon how much it was out of bounds.
    float bounce = BOUNCE_MULTIPLIER * Math.abs(distOutOfBounds / mIrisRadius);
    if (velocity > 0) {
      velocity += bounce * simulationRate;
    } else {
      velocity -= bounce * simulationRate;
    }

    return velocity;
  }

  /**
   * The iris is stopped if it is at the bottom of the eye and its velocity is zero.
   */
  private boolean isStopped() {
    if (mEyePosition.y >= mIrisPosition.y) {
      return false;
    }

    float irisOffsetY = mIrisPosition.y - mEyePosition.y;
    float maxDistance = mEyeRadius - mIrisRadius;
    if (irisOffsetY < maxDistance) {
      return false;
    }

    return (isZero(vx) && isZero(vy));
  }

  /**
   * Allow for a small tolerance in floating point values in considering whether a value is zero.
   */
  private boolean isZero(float num) {
    return ((num < ZERO_TOLERANCE) && (num > -1 * ZERO_TOLERANCE));
  }
}
