import pandas as pd
import datetime

def filter():

    df = pd.read_csv("inputs/Chicago_Traffic_Crashes_2018_Jul_Aug.csv", parse_dates=[2])
    #df = pd.read_json("https://data.cityofchicago.org/resource/gbzg-imeg.json")

    # maxlat: 41.89161, minlat: 41.87202, maxlon: -87.62069, minlon: -87.64807
    # filter by bounding box
    df = df[(41.87202 < df['LATITUDE']) & (df['LATITUDE'] < 41.89161) & (-87.64807 < df['LONGITUDE']) & (df['LONGITUDE'] < -87.62069)]

    # filter only July data
    df = df[(df['CRASH_DATE'] >= datetime.date(2018, 7, 1)) & (df['CRASH_DATE'] <= datetime.date(2018, 7, 31))]

    df.to_csv("inputs/Chicago_Traffic_Crashes_2018_Jul_filtered.csv")


if __name__ == "__main__":

    filter()