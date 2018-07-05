import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
import sys
import re
from collections import defaultdict

parsed = {method:defaultdict(list) for method in ["existential", "variance", "combined"]}
nodelimit = None
edgelimit = None

base = sys.argv[1]

data = pd.read_csv("results_" + base + "_" + "existential.csv", sep=",").fillna('')
for row in data.iterrows():
    nodelimit = row[1]["NodeThreshold"]
    edgelimit = row[1]["EdgeThreshold"]
    break

def read_method(method, parsed):
    global base
    data = pd.read_csv("results_" + base + "_" + method + ".csv", sep=",").fillna('')
    for row in data.iterrows():
        parsed[method]["objective"] += [row[1]["Objective"] / 50]
        parsed[method]["sedges"] += [row[1]["SummaryEdges"]]
        parsed[method]["snodes"] += [row[1]["SummaryNodes"]]
        parsed[method]["test"] += [row[1]["TestObjective"] / 20]

read_method("existential", parsed)
read_method("variance", parsed)
read_method("combined", parsed)


c = {0: "#2ca02c", 1: "#1f77b4", 2: "#ff7f0e"}#, "3": "#d62728"}

for i, method in enumerate(parsed):
    plt.plot(parsed[method]["snodes"], parsed[method]["objective"], label = method, color = c[i])
    plt.plot(parsed[method]["snodes"], parsed[method]["test"], label = method, linestyle="--", color = c[i])
plt.legend(loc="upper right",fancybox=True)
plt.xlabel("number of nodes in the summary")
plt.ylabel("Objective function value")
#plt.axvline(x=nodelimit/2)
plt.gca().set_ylim(bottom=0)
plt.savefig(base + "nodesobj.pdf", dpi=600)
plt.show()

for i, method in enumerate(parsed):
    plt.plot(parsed[method]["sedges"], parsed[method]["objective"], label = method, color = c[i])
    plt.plot(parsed[method]["sedges"], parsed[method]["test"], label = method, linestyle="--", color=c[i])
plt.legend(loc="upper right",fancybox=True)
plt.xlabel("number of edges in the summary")
plt.ylabel("Objective function value")
#plt.axvline(x=edgelimit/2)
plt.gca().set_ylim(bottom=0)
plt.savefig(base + "edgesobj.pdf", dpi=600)
plt.show()


