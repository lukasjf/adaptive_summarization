import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
import sys
import re
from collections import defaultdict

ftcm = pd.read_csv("tcm.csv", sep=",").fillna('')
frdfsum = pd.read_csv("rdfsum.csv", sep=",").fillna('')
fcache = pd.read_csv("cache.csv", sep=",").fillna('')
fmerge = pd.read_csv("merged.csv", sep=",").fillna('')

benchmarks = ["query1", "query01", "query001", "query033", "query0033"]
storages = [1350000, 675000, 337500, 168750]

benchmark = sys.argv[1]

tcm = defaultdict(dict)
rdf = defaultdict(dict)
cache = defaultdict(dict)
merge = defaultdict(dict)
 
for row in ftcm.iterrows():
    if benchmark not in row[1]["queryset"]:
        continue
    storage = row[1]["storage"]
    tcm[storage]["trf1"] = float(row[1]["trainingF1"])
    tcm[storage]["tf1"] = float(row[1]["testF1"])

for row in frdfsum.iterrows():
    if benchmark not in row[1]["queryset"]:
        continue
    for storage in storages:
        rdf[storage]["trf1"] = float(row[1]["trainingF1"])
        rdf[storage]["tf1"] = float(row[1]["testF1"])


for row in fcache.iterrows():
    if benchmark not in row[1]["queryset"]:
        continue
    storage = row[1]["storage"]
    if not storage in cache:
        cache[storage]["trf1"] = []
        cache[storage]["tf1"] = []
    cache[storage]["trf1"] += [float(row[1]["trainingF1"])]
    cache[storage]["tf1"] += [float(row[1]["testF1"])]

for row in fmerge.iterrows():
    if benchmark not in row[1]["queryset"]:
        continue
    storage = row[1]["storage"]
    if not storage in merge:
        merge[storage]["trf1"] = []
        merge[storage]["tf1"] = []
    merge[storage]["trf1"] += [float(row[1]["trainingF1"])]
    merge[storage]["tf1"] += [float(row[1]["testF1"])]


methods = [tcm, rdf]
colors = ["y", "r", "b", "g"]

for i in range(len(methods)):
    method = methods[i]
    print(method)
    x = np.arange(len(storages))    
    ytrain = [method[key]["trf1"] for key in method.keys()]
    ytest = [method[key]["tf1"] for key in method.keys()]
    print(ytrain)
    plt.bar(x + 0.1 * i, ytrain, 0.1, color = colors[i])
    plt.bar(x + 0.5 + 0.1 * i, ytest, 0.1, color = colors[i]) 

methods = [cache, merge]

for i in range(len(methods)):
    method = methods[i]
    x = np.arange(len(storages))    
    ytrain = [np.mean(method[key]["trf1"]) for key in method.keys()]
    ytrainstd = [np.std(method[key]["trf1"]) for key in method.keys()]
    ytest = [np.mean(method[key]["tf1"]) for key in method.keys()]
    yteststd = [np.std(method[key]["tf1"]) for key in method.keys()]
    plt.bar(x + 0.2 + 0.1 * i, ytrain, 0.1, color = colors[i+2], yerr=ytrainstd)
    plt.bar(x + 0.7 + 0.1 * i, ytest, 0.1, color = colors[i+2], yerr=yteststd) 


ticks = storages
plt.xticks(np.arange(len(ticks)) + 0.45, ticks)
plt.gca().set_ylim(bottom=0)
plt.savefig(benchmark + ".pdf")
plt.show()
    
    









