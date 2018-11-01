# Created by Saurav Satpathy on 25/10/18.
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ==============================================================================

import tfcoreml
tfcoreml.convert(tf_model_path='retrained_graph.pb',
                 mlmodel_path='indian_food.mlmodel',
                 input_name_shape_dict={'Placeholder:0':[1,299,299,3]},
                 output_feature_names=['final_result:0'],
                 image_input_names = 'input:0',
                 class_labels = 'retrained_labels.txt',
                 red_bias = -1,
                 green_bias = -1,
                 blue_bias = -1,
                 image_scale = 2.0/255.0
                 )
