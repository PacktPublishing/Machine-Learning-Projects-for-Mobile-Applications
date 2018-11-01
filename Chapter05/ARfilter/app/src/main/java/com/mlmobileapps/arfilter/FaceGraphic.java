package com.mlmobileapps.arfilter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.util.Log;

/**
 * Graphic instance for rendering face position, orientation, and landmarks within an associated
 * graphic overlay view.
 */
public class FaceGraphic extends GraphicOverlay.Graphic {

    private final String TAG = "FaceGraphic";

    private Bitmap marker;

    private static final float DOT_RADIUS = 3.0f;
    private static final float TEXT_OFFSET_Y = -30.0f;

    private boolean mIsFrontFacing;

    // This variable may be written to by one of many threads. By declaring it as volatile,
    // we guarantee that when we read its contents, we're reading the most recent "write"
    // by any thread.
    private volatile FaceData mFaceData;

    private Paint mHintTextPaint;
    private Paint mHintOutlinePaint;
    private Paint mEyeWhitePaint;
    private Paint mIrisPaint;
    private Paint mEyeOutlinePaint;
    private Paint mEyelidPaint;

    private Drawable mPigNoseGraphic;
    private Drawable mMustacheGraphic;
    private Drawable mHappyStarGraphic;
    private Drawable mHatGraphic;
    private Drawable mAngry;
    private Drawable mDisgust_leftEye;
    private Drawable mDisgust_rightEye;
    private Drawable mDisgust_mouth;
    private Drawable mFear_left;
    private Drawable mFear_right;
    private Drawable mSad_left;
    private Drawable mSad_right;
    private Drawable mSurprise_leftEye;
    private Drawable mSurprise_rightEye;
    private Drawable mSurprise_mouth;
    private Drawable mFemale_wig;
    private Drawable mGirl_left_eye;
    private Drawable mGirl_right_eye;
    private Drawable mFear_mouth;

    // We want each iris to move independently, so each one gets its own physics engine.
    private EyePhysics mLeftPhysics = new EyePhysics();
    private EyePhysics mRightPhysics = new EyePhysics();

    private final String classifierType = ARFilterActivity.classifierType();

    FaceGraphic(GraphicOverlay overlay, Context context, boolean isFrontFacing) {
        super(overlay);
        Log.d(TAG,"Inside Constructor");
        mIsFrontFacing = isFrontFacing;
        Resources resources = context.getResources();
        initializePaints(resources);
        initializeGraphics(resources);
    }

    private void initializeGraphics(Resources resources) {
        Log.d(TAG,"Initializing resources");

        mPigNoseGraphic = resources.getDrawable(R.drawable.pig_nose_emoji);
        mMustacheGraphic = resources.getDrawable(R.drawable.mustache);
        mHappyStarGraphic = resources.getDrawable(R.drawable.happy_star);
        mHatGraphic = resources.getDrawable(R.drawable.hat);

        mAngry = resources.getDrawable(R.drawable.angry);
        mDisgust_leftEye = resources.getDrawable(R.drawable.disgust_left);
        mDisgust_rightEye = resources.getDrawable(R.drawable.disgust_right);
        mDisgust_mouth = resources.getDrawable(R.drawable.disgust_mouth);
        mFear_left = resources.getDrawable(R.drawable.fear_left);
        mFear_right = resources.getDrawable(R.drawable.fear_right);
        mFear_mouth = resources.getDrawable(R.drawable.fear_mouth);
        mSad_left = resources.getDrawable(R.drawable.sad_left);
        mSad_right = resources.getDrawable(R.drawable.sad_right);
        mSurprise_leftEye = resources.getDrawable(R.drawable.surprise_left_eye);
        mSurprise_rightEye = resources.getDrawable(R.drawable.surprise_right_eye);
        mSurprise_mouth = resources.getDrawable(R.drawable.surprise_mouth);
        mFemale_wig = resources.getDrawable(R.drawable.female_wig);
        mGirl_left_eye = resources.getDrawable(R.drawable.girl_eye_left);
        mGirl_right_eye = resources.getDrawable(R.drawable.girl_eye_right);
    }

    private void initializePaints(Resources resources) {
        Log.d(TAG,"Initialize Paints");

        mHintTextPaint = new Paint();
        mHintTextPaint.setColor(resources.getColor(R.color.overlayHint));
        mHintTextPaint.setTextSize(resources.getDimension(R.dimen.textSize));

        mHintOutlinePaint = new Paint();
        mHintOutlinePaint.setColor(resources.getColor(R.color.overlayHint));
        mHintOutlinePaint.setStyle(Paint.Style.STROKE);
        mHintOutlinePaint.setStrokeWidth(resources.getDimension(R.dimen.hintStroke));

        mEyeWhitePaint = new Paint();
        mEyeWhitePaint.setColor(resources.getColor(android.R.color.transparent));
        mEyeWhitePaint.setStyle(Paint.Style.FILL);

        mIrisPaint = new Paint();
        mIrisPaint.setColor(resources.getColor(R.color.colorAccent));
        mIrisPaint.setStyle(Paint.Style.FILL);

        mEyeOutlinePaint = new Paint();
        mEyeOutlinePaint.setColor(resources.getColor(R.color.eyeOutline));
        mEyeOutlinePaint.setStyle(Paint.Style.STROKE);
        mEyeOutlinePaint.setStrokeWidth(resources.getDimension(R.dimen.eyeOutlineStroke));

        mEyelidPaint = new Paint();
        mEyelidPaint.setColor(resources.getColor(R.color.eyelid));
        mEyelidPaint.setStyle(Paint.Style.FILL);
    }

    // 1
    void update(FaceData faceData) {
        Log.d(TAG,"Update called");
        mFaceData = faceData;
        postInvalidate(); // Trigger a redraw of the graphic (i.e. cause draw() to be called).
    }

    @Override
    public void draw(Canvas canvas) {

        Log.d(TAG,"Inside onDraw");

        final float DOT_RADIUS = 3.0f;
        final float TEXT_OFFSET_Y = -30.0f;

        // Confirm that the face and its features are still visible before drawing any graphics over it.
        if (mFaceData == null) {
            return;
        }

        // 1
        PointF detectPosition = mFaceData.getPosition();
        PointF detectLeftEyePosition = mFaceData.getLeftEyePosition();
        PointF detectRightEyePosition = mFaceData.getRightEyePosition();
        PointF detectNoseBasePosition = mFaceData.getNoseBasePosition();
        PointF detectMouthLeftPosition = mFaceData.getMouthLeftPosition();
        PointF detectMouthBottomPosition = mFaceData.getMouthBottomPosition();
        PointF detectMouthRightPosition = mFaceData.getMouthRightPosition();
        if ((detectPosition == null) ||
                (detectLeftEyePosition == null) ||
                (detectRightEyePosition == null) ||
                (detectNoseBasePosition == null) ||
                (detectMouthLeftPosition == null) ||
                (detectMouthBottomPosition == null) ||
                (detectMouthRightPosition == null)) {
            return;
        }

        // Face position and dimensions
        PointF position = new PointF(translateX(detectPosition.x),
                translateY(detectPosition.y));
        float width = scaleX(mFaceData.getWidth());
        float height = scaleY(mFaceData.getHeight());

        // Eye state
        boolean leftEyeOpen = mFaceData.isLeftEyeOpen();
        boolean rightEyeOpen = mFaceData.isRightEyeOpen();

        // Eye coordinates
        PointF leftEyePosition = new PointF(translateX(detectLeftEyePosition.x),
                translateY(detectLeftEyePosition.y));
        PointF rightEyePosition = new PointF(translateX(detectRightEyePosition.x),
                translateY(detectRightEyePosition.y));

        // Nose coordinates
        PointF noseBasePosition = new PointF(translateX(detectNoseBasePosition.x),
                translateY(detectNoseBasePosition.y));

        // Mouth coordinates
        PointF mouthLeftPosition = new PointF(translateX(detectMouthLeftPosition.x),
                translateY(detectMouthLeftPosition.y));
        PointF mouthRightPosition = new PointF(translateX(detectMouthRightPosition.x),
                translateY(detectMouthRightPosition.y));
        PointF mouthBottomPosition = new PointF(translateX(detectMouthBottomPosition.x),
                translateY(detectMouthBottomPosition.y));

        // Forehead coordinates
        float eyedist = Math.abs(leftEyePosition.x - rightEyePosition.x);
        float eyemidPoint = Math.abs(leftEyePosition.x + rightEyePosition.x)/2;
        float leftX = leftEyePosition.x - 2.5f*eyemidPoint;
        float rightX = rightEyePosition.x + eyemidPoint;
        float topY = leftEyePosition.y - 2*eyedist;
        float bottomY = mouthBottomPosition.y+0.5f*eyemidPoint;
        PointF headTop = new PointF(leftX,topY);
        PointF headBottom = new PointF(rightX,bottomY);

        // Smile state
        boolean smiling = mFaceData.isSmiling();

        // Calculate the distance between the eyes using Pythagoras' formula,
        // and we'll use that distance to set the size of the eyes and irises.

        final float EYE_RADIUS_PROPORTION = 0.45f;
        final float IRIS_RADIUS_PROPORTION = EYE_RADIUS_PROPORTION / 2.0f;
        float distance = (float) Math.sqrt(
                (rightEyePosition.x - leftEyePosition.x) * (rightEyePosition.x - leftEyePosition.x) +
                        (rightEyePosition.y - leftEyePosition.y) * (rightEyePosition.y - leftEyePosition.y));
        float eyeRadius = EYE_RADIUS_PROPORTION * distance;
        float irisRadius = IRIS_RADIUS_PROPORTION * distance;

        // Draw the eyes.
        PointF leftIrisPosition = mLeftPhysics.nextIrisPosition(leftEyePosition, eyeRadius, irisRadius);
//        drawEye(canvas, leftEyePosition, eyeRadius, leftIrisPosition, irisRadius, leftEyeOpen, smiling);
        PointF rightIrisPosition = mRightPhysics.nextIrisPosition(rightEyePosition, eyeRadius, irisRadius);
//        drawEye(canvas, rightEyePosition, eyeRadius, rightIrisPosition, irisRadius, rightEyeOpen, smiling);

//        find mouth top co-ordinate
        float mouthMidPointX = (float)((mouthRightPosition.x + mouthLeftPosition.x)/2);
        float xtemp = mouthMidPointX - mouthBottomPosition.x;
        float ytemp = Math.abs(mouthLeftPosition.y - mouthBottomPosition.y);
        float lowerMouthdistance = (float) Math.sqrt(xtemp*xtemp + ytemp*ytemp);

        float upperMouthx = mouthBottomPosition.x;
        float upperMouthy = mouthLeftPosition.y - lowerMouthdistance;
        PointF mouthTopPosition = new PointF(upperMouthx,upperMouthy);

        String Type = ImageClassifier.getTopLabel();

        if(classifierType.equals("emotion")) {
            switch (Type) {
                case "anger":
                 //   drawEye(mAngry, canvas, leftEyePosition, eyeRadius, leftIrisPosition, irisRadius, leftEyeOpen, smiling);
                  //  drawEye(mAngry, canvas, rightEyePosition, eyeRadius, rightIrisPosition, irisRadius, rightEyeOpen, smiling);
                    break;

                case "disgust":
//                    drawEye(mDisgust_leftEye, canvas, leftEyePosition, eyeRadius, leftIrisPosition, irisRadius, leftEyeOpen, smiling);
//                    drawEye(mDisgust_rightEye, canvas, rightEyePosition, eyeRadius, rightIrisPosition, irisRadius, rightEyeOpen, smiling);
//                    drawNose(canvas,noseBasePosition,leftEyePosition,rightEyePosition,width);
//                    drawMouth(mDisgust_mouth, canvas, mouthLeftPosition, mouthRightPosition, mouthBottomPosition, mouthTopPosition);
                    break;

                case "fear":
//                    drawEye(mFear_left, canvas, leftEyePosition, eyeRadius, leftIrisPosition, irisRadius, leftEyeOpen, smiling);
//                    drawEye(mFear_right, canvas, rightEyePosition, eyeRadius, rightIrisPosition, irisRadius, rightEyeOpen, smiling);
//                    drawMouth(mFear_mouth,canvas,mouthLeftPosition,mouthRightPosition,mouthBottomPosition,mouthTopPosition);
                    break;
                case "happiness":
                  //  drawEye(mHappyStarGraphic, canvas, leftEyePosition, eyeRadius, leftIrisPosition, irisRadius, leftEyeOpen, smiling);
                  //  drawEye(mHappyStarGraphic, canvas, rightEyePosition, eyeRadius, rightIrisPosition, irisRadius, rightEyeOpen, smiling);
                    break;

                case "sadness":
                  //  drawEye(mSad_left, canvas, leftEyePosition, eyeRadius, leftIrisPosition, irisRadius, leftEyeOpen, smiling);
                  //  drawEye(mSad_right, canvas, rightEyePosition, eyeRadius, rightIrisPosition, irisRadius, rightEyeOpen, smiling);
                    break;

                case "surprise":
                  //  drawEye(mSurprise_leftEye, canvas, leftEyePosition, eyeRadius, leftIrisPosition, irisRadius, leftEyeOpen, smiling);
                  //  drawEye(mSurprise_rightEye, canvas, rightEyePosition, eyeRadius, rightIrisPosition, irisRadius, rightEyeOpen, smiling);
                  //  drawMouth(mSurprise_mouth, canvas, mouthLeftPosition, mouthRightPosition, mouthBottomPosition, mouthTopPosition);
                    break;
            }
        }
        else if(classifierType.equals("gender")){
            switch (Type){
                case "male":
                    drawMoustache(canvas,noseBasePosition,mouthLeftPosition,mouthRightPosition);
                             break;
                case "female":
                              // drawEye(mGirl_left_eye,canvas,leftEyePosition,eyeRadius,leftIrisPosition,irisRadius,leftEyeOpen,smiling);
                               // drawEye(mGirl_right_eye,canvas,rightEyePosition,eyeRadius,rightIrisPosition,irisRadius,rightEyeOpen,smiling);
                               // drawForeHead(mFemale_wig,canvas,headTop,headBottom);
                    drawHat(canvas, position, width, height, noseBasePosition);
                                break;
            }
        }

        // Draw the nose.
   //     drawNose(canvas, noseBasePosition, leftEyePosition, rightEyePosition, width);

        // Draw the mustache.
   //     drawMoustache(canvas, noseBasePosition, mouthLeftPosition, mouthRightPosition);

        // 2
//        float leftEyeX = translateX(detectLeftEyePosition.x);
//        float leftEyeY = translateY(detectLeftEyePosition.y);
//        canvas.drawCircle(leftEyeX, leftEyeY, DOT_RADIUS, mHintOutlinePaint);
//        canvas.drawText("left eye", leftEyeX, leftEyeY + TEXT_OFFSET_Y, mHintTextPaint);
//
//        float rightEyeX = translateX(detectRightEyePosition.x);
//        float rightEyeY = translateY(detectRightEyePosition.y);
//        canvas.drawCircle(rightEyeX, rightEyeY, DOT_RADIUS, mHintOutlinePaint);
//        canvas.drawText("right eye", rightEyeX, rightEyeY + TEXT_OFFSET_Y, mHintTextPaint);
//
//        float noseBaseX = translateX(detectNoseBasePosition.x);
//        float noseBaseY = translateY(detectNoseBasePosition.y);
//        canvas.drawCircle(noseBaseX, noseBaseY, DOT_RADIUS, mHintOutlinePaint);
//        canvas.drawText("nose base", noseBaseX, noseBaseY + TEXT_OFFSET_Y, mHintTextPaint);
//
//        float mouthLeftX = translateX(detectMouthLeftPosition.x);
//        float mouthLeftY = translateY(detectMouthLeftPosition.y);
//        canvas.drawCircle(mouthLeftX, mouthLeftY, DOT_RADIUS, mHintOutlinePaint);
//        canvas.drawText("mouth left", mouthLeftX, mouthLeftY + TEXT_OFFSET_Y, mHintTextPaint);
//
//        float mouthRightX = translateX(detectMouthRightPosition.x);
//        float mouthRightY = translateY(detectMouthRightPosition.y);
//        canvas.drawCircle(mouthRightX, mouthRightY, DOT_RADIUS, mHintOutlinePaint);
//        canvas.drawText("mouth right", mouthRightX, mouthRightY + TEXT_OFFSET_Y, mHintTextPaint);
//
//        float mouthBottomX = translateX(detectMouthBottomPosition.x);
//        float mouthBottomY = translateY(detectMouthBottomPosition.y);
//        canvas.drawCircle(mouthBottomX, mouthBottomY, DOT_RADIUS, mHintOutlinePaint);
//        canvas.drawText("mouth bottom", mouthBottomX, mouthBottomY + TEXT_OFFSET_Y, mHintTextPaint);

        // Head tilt
        float eulerY = mFaceData.getEulerY();
        float eulerZ = mFaceData.getEulerZ();

        // Draw the hat only if the subject's head is titled at a sufficiently jaunty angle.
//        final float HEAD_TILT_HAT_THRESHOLD = 5.0f;
//        if (Math.abs(eulerZ) > HEAD_TILT_HAT_THRESHOLD) {
//            drawHat(canvas, position, width, height, noseBasePosition);
//        }
    }

    private void drawEye(Drawable drawable, Canvas canvas,
                         PointF eyePosition, float eyeRadius,
                         PointF irisPosition, float irisRadius,
                         boolean eyeOpen, boolean smiling) {
        Log.d(TAG,"Inside DrawEye");
        if (eyeOpen) {
            canvas.drawCircle(eyePosition.x, eyePosition.y, eyeRadius, mEyeWhitePaint);

            drawable.setBounds(
                    (int)(irisPosition.x - irisRadius),
                    (int)(irisPosition.y - irisRadius),
                    (int)(irisPosition.x + irisRadius),
                    (int)(irisPosition.y + irisRadius));
            drawable.draw(canvas);

            if (smiling) {
                drawable.setBounds(
                        (int)(irisPosition.x - irisRadius),
                        (int)(irisPosition.y - irisRadius),
                        (int)(irisPosition.x + irisRadius),
                        (int)(irisPosition.y + irisRadius));
                drawable.draw(canvas);
            } else {
                canvas.drawCircle(irisPosition.x, irisPosition.y, irisRadius, mIrisPaint);
            }
        } else {
            canvas.drawCircle(eyePosition.x, eyePosition.y, eyeRadius, mEyelidPaint);
            float y = eyePosition.y;
            float start = eyePosition.x - eyeRadius;
            float end = eyePosition.x + eyeRadius;
            canvas.drawLine(start, y, end, y, mEyeOutlinePaint);
        }
        canvas.drawCircle(eyePosition.x, eyePosition.y, eyeRadius, mEyeOutlinePaint);
    }

    private void drawNose(Canvas canvas,
                          PointF noseBasePosition,
                          PointF leftEyePosition, PointF rightEyePosition,
                          float faceWidth) {
        final float NOSE_FACE_WIDTH_RATIO = (float)(1 / 5.0);
        float noseWidth = faceWidth * NOSE_FACE_WIDTH_RATIO;
        int left = (int)(noseBasePosition.x - (noseWidth / 2));
        int right = (int)(noseBasePosition.x + (noseWidth / 2));
        int top = (int)(leftEyePosition.y + rightEyePosition.y) / 2;
        int bottom = (int)noseBasePosition.y;

        mPigNoseGraphic.setBounds(left, top, right, bottom);
        mPigNoseGraphic.draw(canvas);
    }

    private void drawForeHead(Drawable drawable, Canvas canvas, PointF topHead, PointF bottomHead){
        int left = (int)topHead.x;
        int bottom = (int)bottomHead.y;
        int right = (int)bottomHead.x;
        int top = (int)topHead.y;

        if(mIsFrontFacing){
            drawable.setBounds(left,top,right,bottom);
        }
        else
        {
            drawable.setBounds(right,top,left,bottom);
        }
        drawable.draw(canvas);
    }

    private void drawMoustache(Canvas canvas,
                               PointF noseBasePosition,
                               PointF mouthLeftPosition, PointF mouthRightPosition) {
        int left = (int)mouthLeftPosition.x;
        int top = (int)noseBasePosition.y;
        int right = (int)mouthRightPosition.x;
        int bottom = (int) Math.min(mouthLeftPosition.y, mouthRightPosition.y);

        if (mIsFrontFacing) {
            mMustacheGraphic.setBounds(left, top, right, bottom);
        } else {
            mMustacheGraphic.setBounds(right, top, left, bottom);
        }
        mMustacheGraphic.draw(canvas);
    }

    private void drawMouth(Drawable drawable, Canvas canvas, PointF mouthLeftPos, PointF mouthRightPos, PointF mouthBottomPos, PointF mouthTopPos){

        int left = (int)mouthLeftPos.x;
        int bottom = (int)mouthBottomPos.y;
        int right = (int)mouthRightPos.x;
        int top = (int)mouthTopPos.y;

        if(mIsFrontFacing){
            drawable.setBounds(left,top,right,bottom);
        }
        else
        {
            drawable.setBounds(right,top,left,bottom);
        }
        drawable.draw(canvas);
    }

    private void drawHat(Canvas canvas, PointF facePosition, float faceWidth, float faceHeight, PointF noseBasePosition) {
        final float HAT_FACE_WIDTH_RATIO = (float)(4.0 / 4.0);
        final float HAT_FACE_HEIGHT_RATIO = (float)(3.0 / 6.0);
        final float HAT_CENTER_Y_OFFSET_FACTOR = (float)(1.0 / 8.0);

        float hatCenterY = facePosition.y + (faceHeight * HAT_CENTER_Y_OFFSET_FACTOR);
        float hatWidth = faceWidth * HAT_FACE_WIDTH_RATIO;
        float hatHeight = faceHeight * HAT_FACE_HEIGHT_RATIO;

        int left = (int)(noseBasePosition.x - (hatWidth / 2));
        int right = (int)(noseBasePosition.x + (hatWidth / 2));
        int top = (int)(hatCenterY - (hatHeight / 2));
        int bottom = (int)(hatCenterY + (hatHeight / 2));
        mHatGraphic.setBounds(left, top, right, bottom);
        mHatGraphic.draw(canvas);
    }
}
