import sys
import json
import cv2
import tensorflow as tf
from tensorflow.keras.models import load_model
import numpy as np
from matplotlib import pyplot as plt
import warnings
import os
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '3' 
warnings.filterwarnings("ignore") 

def imageProcess(path):
    #loading model to the env
    model = load_model('models/imageclassifier.h5')
    IMAGE_SIZE=(512,720) 
    #loading image to the env
    img = cv2.imread(path)  
    
    img=cv2.cvtColor(img,cv2.COLOR_BGR2RGB)
    resize = tf.image.resize(img, IMAGE_SIZE)
    yhat = model.predict(np.expand_dims(resize/255, 0),verbose = 0) 
    # plt.imshow(resize.numpy().astype(int))
    # plt.show()
    yhat=yhat[0][0]
    if yhat > 0.5: 
        print("Predicted value is Original and the accuracy is {:.2f}%".format(yhat*100))
    else:
        print("Predicted value is Fake and the accuracy is {:.2f}%".format(yhat*100)) 

functionMapper={
    "imageProcess":imageProcess
}
if(__name__=="__main__"):
    req=json.loads(sys.argv[1]) 
    functionMapper[req["method"]](req["path"])