# Vehicle accidents are clustered and correlated with road attributes

Vehicle accident data is obtained from Chicago city data portal:
https://data.cityofchicago.org/Transportation/Traffic-Crashes-Vehicles/68nd-jvt3

Clustering is done by DBScan.

Road attribute data is extracted from HERE Map Content using OLP SDK 1.6.1.2.

Result: Display raw data and clustered data to see if there is any correlation such as injury frequency vs number of lanes.

XYZ display: https://xyz.here.com/viewer/?project_id=e9bdf580-a638-11e8-9721-59af634cacdd

Key features on XYZ display:
  1. The geojson displays number of lanes as lines on the roads according to the Lane count. 
  2. The pop up box contains all other information. If the road is clicked, road attributes are displayed such as TileID, SegmentID, functional_class, speed_limit, lane_count, etc. extracted from HERE Map Content. If the circles are clicked, the accident related information is displayed.
  3. The csv file contains Chicago vehicle crash data happened in July 2018 filtered by the given area. 
  4. The clustering outputs are grouped by outline circles with different colors. 
  5. Total 25 clusters were found, but eue to the number of space limit on XYZ dataset, only the first 5 clusters are shown.

![road_attributes](https://github.com/dwjang/xyzhackathon/blob/master/popup-roadattributes.png)
![crash_attributes](https://github.com/dwjang/xyzhackathon/blob/master/popup-accidentattributes.png)

Files: 

  inputs: local copy of Chicago Traffic Crashes data and filtered data
  results: clustering output files as csv
  filter.py: filter crash data for July 2018 only
  clustering.py: perform DBScan clustering

  RoadAttributes.scala:
    RoadAttributes.scala is supposed to be run within HERE OLP SDK 1.6.1.2.
    It extracts various road attributes from a given latitude, longitude, and radius.
    The extracted information is:
      Tile ID: zoom level 14
      Segment ID
      Direction
      offset_start
      offset_end
      Functional Class
      Lane Count
      Speed Limit
      Offset start/end are necessary in case the attributes (Functional Class, Lane Count, Speed Limit) are changed along a segment.
    output: Chicago_Lanes.geojson

