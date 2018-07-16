import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
import sys
import re
from collections import defaultdict


q01 = pd.read_csv("tdcomb005.csv",sep=",").fillna('')

tr = []
test = []

for row in q01.iterrows():
    tr += [float(row[1]["trainingF1"])]
    test += [float(row[1]["testF1"])]

print(tr, test,)
print(np.mean(tr), np.std(tr))

print(np.mean(test), np.std(test))
