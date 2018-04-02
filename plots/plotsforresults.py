import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
import sys
import re
from collections import defaultdict

parsed = {method:defaultdict(list) for method in ["existential", "variance", "combined"]}
nodelimit = None
edgelimit = None

base = "kdd"

data = pd.read_csv("results_" + base + "_" + "existential", sep=",").fillna('')
for row in data.iterrows():
    nodelimit = row[1]["NodeThreshold"]
    edgelimit = row[1]["EdgeThreshold"]


data = pd.read_csv("results_" + base + "_" + "existential", sep=",").fillna('')
for row in data.iterrows():
    parsed["existential"]["objective"] += [row[1]["Objective"]]
    parsed["existential"]["sedges"] += [row[1]["SummaryEdges"]]
    parsed["existential"]["snodes"] += [row[1]["SummaryNodes"]]

data = pd.read_csv("results_" + base + "_" + "variance", sep=",").fillna('')
for row in data.iterrows():
    parsed["variance"]["objective"] += [row[1]["Objective"]]
    parsed["variance"]["sedges"] += [row[1]["SummaryEdges"]]
    parsed["variance"]["snodes"] += [row[1]["SummaryNodes"]]

data = pd.read_csv("results_" + base + "_" + "combined", sep=",").fillna('')
for row in data.iterrows():
    parsed["combined"]["objective"] += [row[1]["Objective"]]
    parsed["combined"]["sedges"] += [row[1]["SummaryEdges"]]
    parsed["combined"]["snodes"] += [row[1]["SummaryNodes"]]


for method in parsed:
    plt.scatter(parsed[method]["snodes"], parsed[method]["objective"], label = method)
plt.legend(loc="upper right",fancybox=True)
plt.xlabel("number of nodes in the summary")
plt.ylabel("Objective function value")
plt.axvline(x=nodelimit)
plt.savefig(base + "nodesobj.pdf", dpi=600)
plt.show()

for method in parsed:
    plt.scatter(parsed[method]["sedges"], parsed[method]["objective"], label = method)
plt.legend(loc="upper right",fancybox=True)
plt.xlabel("number of edges in the summary")
plt.ylabel("Objective function value")
plt.axvline(x=edgelimit)
plt.savefig(base + "edgesobj.pdf", dpi=600)
plt.show()

for method in parsed:
    plt.scatter(parsed[method]["snodes"], parsed[method]["sedges"], label = method)
plt.legend(loc="upper right",fancybox=True)
plt.xlabel("number of nodes in the summary")
plt.ylabel("number of edges in the summary")
plt.axvline(x=nodelimit)
plt.axhline(y=edgelimit)
plt.savefig(base + "nodesedges.pdf", dpi=600)
plt.show()


