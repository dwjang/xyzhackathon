import datetime, math
import pandas as pd
import numpy as np
from sklearn.cluster import DBSCAN


def sinosoidal_projection(point, longitude_reference = 0):

    """
    :param point: (latitude, longitude)
    :param longitude_reference: longitude in degree. Default is 0.
    :return: projected_point: (x, y)

    sinosoidal projection : https://en.wikipedia.org/wiki/Sinusoidal_projection

    x = (longitude - longitude_reference) * cos(latitude)
    y = latitude

    Note:
         1. latitude, longitude are all in radian
         2. point: (latitude, longitude) while projected_point: (x, y)
    """

    lat = math.radians(point[0])
    lon = math.radians(point[1])
    y = lat
    x = (lon - longitude_reference) * math.cos(lat)
    return (x,y)


def perform_clustering(points):

    projected_points = []
    for p in points:
        pp = sinosoidal_projection(p)
        projected_points.append(pp)

    db = DBSCAN(eps=3e-6, min_samples=3, algorithm="ball_tree")
    labels = db.fit_predict(projected_points)
    return labels


def get_points():

    df = pd.read_csv("inputs/Chicago_Traffic_Crashes_2018_Jul_filtered.csv", parse_dates=[2])

    points = list(zip(df["LATITUDE"].values, df["LONGITUDE"].values))
    return df, points


def analyze(df, points):

    labels = perform_clustering(points)
    nlabels = len(set(labels))
    print("nlabels:", nlabels)

    clustered = {}
    unclustered = []
    for i, label in enumerate(labels):
        if label == -1: unclustered.append(i)
        else: clustered.setdefault(label, []).append(i)

    keys = list(clustered.keys())
    print("nclusters:", len(keys))

    xf = df.iloc[unclustered]
    outname = "results/unclustered.csv"
    xf.to_csv(outname)

    keys.sort()
    for key in keys:
        xf = df.iloc[clustered[key]]
        outname = "results/cluster_%02d.csv" % (key)
        xf.to_csv(outname)


if __name__ == "__main__":

    df, points = get_points()
    print("npoints:", len(points))

    analyze(df, points)
    print("Done")
