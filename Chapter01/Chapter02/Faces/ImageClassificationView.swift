import UIKit

/// View used in `ImageClassificationController`
public final class ImageClassificationView: UIView {
  private(set) public lazy var imageView = UIImageView()
  private(set) public lazy var button: UIButton = self.makeButton()
  private(set) public lazy var label: UILabel = self.makeLabel()

  public override init(frame: CGRect) {
    super.init(frame: frame)
    backgroundColor = .white
    imageView.backgroundColor = .lightGray
    imageView.contentMode = .scaleAspectFill
    imageView.clipsToBounds = true

    let subviews: [UIView] = [imageView, button, label]
    for subview in subviews {
      addSubview(subview)
    }
  }

  public required init?(coder aDecoder: NSCoder) {
    fatalError("init(coder:) has not been implemented")
  }
}

// MARK: - Subviews

private extension ImageClassificationView {
  func makeButton() -> UIButton {
    let button = UIButton(type: .system)
    button.titleLabel?.font = .boldSystemFont(ofSize: 18)
    return button
  }

  func makeLabel() -> UILabel {
    let label = UILabel()
    label.font = .systemFont(ofSize: 18)
    label.textColor = .darkGray
    label.textAlignment = .center
    label.numberOfLines = 0
    return label
  }
}

// MARK: - Layout

extension ImageClassificationView {
  func setupConstraints() {
    imageView.translatesAutoresizingMaskIntoConstraints = false
    imageView.topAnchor.constraint(equalTo: topAnchor).isActive = true
    imageView.widthAnchor.constraint(equalTo: widthAnchor).isActive = true
    imageView.heightAnchor.constraint(equalTo: imageView.widthAnchor).isActive = true

    let margin = CGFloat(10)

    button.translatesAutoresizingMaskIntoConstraints = false
    button.topAnchor.constraint(equalTo: imageView.bottomAnchor, constant: margin).isActive = true
    button.centerXAnchor.constraint(equalTo: centerXAnchor).isActive = true
    button.widthAnchor.constraint(equalToConstant: 60)

    label.translatesAutoresizingMaskIntoConstraints = false
    label.topAnchor.constraint(equalTo: button.bottomAnchor, constant: margin * 2).isActive = true
    label.leadingAnchor.constraint(equalTo: leadingAnchor, constant: margin).isActive = true
    label.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -margin).isActive = true
  }
}
