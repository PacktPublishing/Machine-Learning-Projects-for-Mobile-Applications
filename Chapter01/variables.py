import numpy as np
import tensorflow as tf
from tensorflow.python.framework import ops
ops.reset_default_graph()

sess = tf.Session()

x = tf.placeholder(tf.float32, shape=(4,4))
y = tf.identity(x)

print(x)

rand_array = np.random.rand(4,4)

merged = tf.summary.merge_all()

writer = tf.summary.FileWriter("/Users/karthikeyan/Documents/code/book/python/log", sess.graph)

print(sess.run(y,feed_dict={x:rand_array}))
