//
//  ClassificationServiceProtocol.swift
//  Faces
//
//  Created by Karthikeyan NG on 6/26/18.
//  Copyright © 2018 Vadym Markov. All rights reserved.
//

import Vision

public protocol ClassificationServiceProtocol: class {
    init()
    func setup()
    func classify(image: CIImage)
}

public extension FloatingPoint {
    /// Rounds the double to decimal places value
    func roundTo(places: Int) -> Self {
        let divisor = Self(Int(pow(10.0, Double(places))))
        return (self * divisor).rounded() / divisor
    }
}


public extension ClassificationServiceProtocol {
    /// Handle results of the classification request
    func extractClassificationResult(from request: VNRequest, count: Int) -> String {
        guard let observations = request.results as? [VNClassificationObservation] else {
            return " ¯\\_(ツ)_/¯ "
        }
        return observations
            .prefix(upTo: count)
            .map({ "\($0.identifier) (\($0.confidence.roundTo(places: 3) * 100.0)%)" })
            .joined(separator: "\n\n")
    }
}
