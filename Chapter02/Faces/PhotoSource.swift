import UIKit

/// Delegate protocol used for `PhotoSourceController`
public protocol PhotoSourceControllerDelegate: class {
  /// Sent to the delegate when a photo source was selected
  func photoSourceController(_ controller: PhotoSourceController,
                             didSelectSourceType sourceType: UIImagePickerControllerSourceType)
}

/// Controller used to present a picker where the user can select a source for a photo
public final class PhotoSourceController: UIAlertController {
  /// The controller's delegate
  public weak var delegate: PhotoSourceControllerDelegate?

  public override func viewDidLoad() {
    super.viewDidLoad()
    addAction(forSourceType: .camera, title: "Snap a photo")
    addAction(forSourceType: .savedPhotosAlbum, title: "Photo Album")
    addCancelAction()
  }
}

// MARK: - Actions

private extension PhotoSourceController {
  func addAction(forSourceType sourceType: UIImagePickerControllerSourceType, title: String) {
    let action = UIAlertAction(title: title, style: .default) { [weak self] _ in
      guard let `self` = self else {
        return
      }
      self.delegate?.photoSourceController(self, didSelectSourceType: sourceType)
    }

    addAction(action)
  }

  func addCancelAction() {
    let action = UIAlertAction(title: "Cancel", style: .cancel, handler: nil)
    addAction(action)
  }
}
