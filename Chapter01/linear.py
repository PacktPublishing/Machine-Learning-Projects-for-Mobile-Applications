#from __future__ import print_function

import tensorflow as tf
import matplotlib.pyplot as plt
import numpy

#Random number generator
randnumgen = numpy.random

# Parameters
learning_rate = 0.01
training_steps = 1000

values_X = numpy.asarray([1,2,3,4,5.5,6.75,7.2,8,3.5,4.65,5,1.5,4.32,1.65,6.08])
values_Y = numpy.asarray([50,60,65,78,89,104,111,122,71,85,79,56,81.8,55.5,98.3])
iterations = values_X.shape[0]


# tf float points - graph inputs
X = tf.placeholder("float")
Y = tf.placeholder("float")

# Set the weight and bias
W = tf.Variable(randnumgen.randn(), name="weight")
b = tf.Variable(randnumgen.randn(), name="bias")

# Linear model construction
# y = xw + b
prediction = tf.add(tf.multiply(X, W), b)

# The cost method helps to minimize error for gradient descent. This is called mean sqauared error.
cost = tf.reduce_sum(tf.pow(prediction-Y, 2))/(2*iterations)

#  Note, minimize() knows to modify W and b because Variable objects are trainable=True by default
optimizer = tf.train.GradientDescentOptimizer(learning_rate).minimize(cost)

# Initialize the variables (i.e. assign their default value)
init = tf.global_variables_initializer()

# Start training
with tf.Session() as sess:

    # Run the initializer
    sess.run(init)

    # Fit all training data
    for step in range(training_steps):
        for (x, y) in zip(values_X, values_Y):
            sess.run(optimizer, feed_dict={X: x, Y: y})
            c = sess.run(cost, feed_dict={X: values_X, Y:values_Y})
            print("Step:", '%04d' % (step+1), "cost=", "{:.9f}".format(c), \
                "W=", sess.run(W), "b=", sess.run(b))

    print("Successfully completed!")
    training_cost = sess.run(cost, feed_dict={X: values_X, Y: values_Y})
    print("Training cost=", training_cost, "W=", sess.run(W), "b=", sess.run(b))

    # Graphic display
    plt.plot(values_X, values_Y, 'ro', label='Original data')
    plt.plot(values_X, sess.run(W) * values_X + sess.run(b), label='Fitted line')
    plt.legend()
    plt.show()
