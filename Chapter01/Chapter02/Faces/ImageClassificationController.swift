import UIKit

open class ImageClassificationController<Service: ClassificationServiceProtocol>: UIViewController,
PhotoSourceControllerDelegate, UINavigationControllerDelegate, UIImagePickerControllerDelegate  {
  /// View with image, button and labels
  public private(set) lazy var mainView = ImageClassificationView(frame: .zero)
  /// Service used to perform gender, age and emotion classification
  public let classificationService: Service = .init()
  /// Status bar style
  open override var preferredStatusBarStyle: UIStatusBarStyle {
    return .lightContent
  }

  // MARK: - View lifecycle

  open override func viewDidLoad() {
    super.viewDidLoad()
    mainView.frame = view.bounds
    mainView.frame = CGRect(x: 16, y: 550, width: view.frame.width-32, height: 30)
    mainView.button.setTitle("Select a photo", for: .normal)
    mainView.button.addTarget(self, action: #selector(handleSelectPhotoTap), for: .touchUpInside)
    view.addSubview(mainView)

    mainView.setupConstraints()
    classificationService.setup()
  }

  open override func viewDidLayoutSubviews() {
    super.viewDidLayoutSubviews()
    mainView.frame = view.bounds
  }

  // MARK: - Actions

  /// Present image picker
  @objc private func handleSelectPhotoTap() {
    let sourcePicker = PhotoSourceController()
    sourcePicker.delegate = self
    present(sourcePicker, animated: true)
  }

  // MARK: - PhotoSourceControllerDelegate

  public func photoSourceController(_ controller: PhotoSourceController,
                                    didSelectSourceType sourceType: UIImagePickerControllerSourceType) {
    let imagePicker = UIImagePickerController()
    imagePicker.delegate = self
    imagePicker.allowsEditing = true
    imagePicker.sourceType = sourceType
    present(imagePicker, animated: true)
  }

  // MARK: - UIImagePickerControllerDelegate

  public func imagePickerController(_ picker: UIImagePickerController,
                                    didFinishPickingMediaWithInfo info: [String : Any]) {
    let editedImage = info[UIImagePickerControllerEditedImage] as? UIImage
    guard let image = editedImage, let ciImage = CIImage(image: image) else {
      print("Can't analyze selected photo")
      return
    }

    DispatchQueue.main.async { [weak mainView] in
      mainView?.imageView.image = image
      mainView?.label.text = ""
    }

    picker.dismiss(animated: true)

    // Run Core ML classifier
    DispatchQueue.global(qos: .userInteractive).async { [weak self] in
      self?.classificationService.classify(image: ciImage)
    }
  }
}
