import AVFoundation

/// Delegate protocol used for `VideoCaptureService`
public protocol VideoCaptureServiceDelegate: class {
  /// Sent to the delegate when a sample buffer was received
  func videoCaptureService(_ service: VideoCaptureService,
                           didOutput sampleBuffer: CMSampleBuffer,
                           pixelBuffer: CVPixelBuffer)
  /// Sent to the delegate when an error occured
  func videoCaptureService(_ service: VideoCaptureService, didFailWithError error: Error)
}

/// Service used to capture video output from the camera
public final class VideoCaptureService: NSObject {
  enum Error: Swift.Error {
    case noCaptureDevice
  }

  /// The service's delegate
  public weak var delegate: VideoCaptureServiceDelegate?
  /// Video preview layer
  public let previewLayer: AVCaptureVideoPreviewLayer
  // Create pixel buffer and call the delegate 10 times per second
  private let fps: Int32 = 10
  // Service used to check authorization status of the capture device
  private let permissionService = VideoPermissionService()
  private let session = AVCaptureSession()
  /// Last time the delegate was called
  private var lastTime = CMTime()

  // MARK: - Init

  public override init() {
    self.previewLayer = AVCaptureVideoPreviewLayer(session: self.session)
    super.init()
  }

  // MARK: - Capturing

  /// Chack video permission and start capturing video output (by default back one)
  public func startCapturing(_ position: AVCaptureDevice.Position = .back, orientation: AVCaptureVideoOrientation = .portrait) {
    permissionService.checkPersmission { [weak self] error in
      guard let `self` = self else {
        return
      }
      do {
        if let error = error {
          throw error
        }
        try self.setupCamera(position: position, orientation: orientation)
      } catch {
        self.delegate?.videoCaptureService(self, didFailWithError: error)
      }
    }
  }
}

// MARK: - Setup

private extension VideoCaptureService {
  /// Setup camera input, output, preview layer and start session
  func setupCamera(position: AVCaptureDevice.Position = .back, orientation: AVCaptureVideoOrientation = .portrait) throws {
    session.beginConfiguration()
    session.sessionPreset = .medium

    // Setup input
    guard let device = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: position) else {
      throw Error.noCaptureDevice
    }

    let input = try AVCaptureDeviceInput(device: device)
    session.addInput(input)

    // Setup output
    let output = AVCaptureVideoDataOutput()
    output.videoSettings = [kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA]
    output.alwaysDiscardsLateVideoFrames = true
    output.setSampleBufferDelegate(self, queue: DispatchQueue(label: "VideoCaptureService.SampleBufferQueue"))
    session.addOutput(output)
    output.connections.first?.videoOrientation = orientation

    // Setup preview layer
    previewLayer.videoGravity = .resizeAspectFill
    previewLayer.connection?.videoOrientation = orientation

    // Run session
    session.commitConfiguration()
    session.startRunning()
  }
}

// MARK: - AVCaptureVideoDataOutputSampleBufferDelegate

extension VideoCaptureService: AVCaptureVideoDataOutputSampleBufferDelegate {
  public func captureOutput(_ output: AVCaptureOutput,
                            didOutput sampleBuffer: CMSampleBuffer,
                            from connection: AVCaptureConnection) {
    let time = CMSampleBufferGetPresentationTimeStamp(sampleBuffer)
    guard (time - lastTime) >= CMTime.init(value: 1, timescale: fps) else {
      return
    }

    lastTime = time

    if let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) {
      delegate?.videoCaptureService(self, didOutput: sampleBuffer, pixelBuffer: pixelBuffer)
    }
  }
}
