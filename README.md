# xyzhackathon

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

The geojson displays number of lanes as lines on the roads according to the Lane count. The pop up box contains all other information.
