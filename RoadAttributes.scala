/*
 * Copyright (C) 2017-2018 HERE Global B.V. and its affiliate(s).
 * All rights reserved.
 *
 * This software and other materials contain proprietary information
 * controlled by HERE and are protected by applicable copyright legislation.
 * Any use and utilization of this software and other materials and
 * disclosure to any third parties is conditional upon having a separate
 * agreement with HERE for the access, use, utilization or disclosure of this
 * software. In the absence of such agreement, the use of the software is not
 * allowed.
 */

package com.here.platform.example.location.scala.standalone

import java.nio.file.Files

import com.here.platform.example.location.utils.ExampleConstants._
import com.here.platform.example.location.utils.Visualization.{Color, RGB}
import com.here.platform.example.location.utils.{FileNameHelper, Visualization}
import com.here.platform.location.compilation.heremapcontent.{AttributeAccessor, AttributeAccessors}
import com.here.platform.location.core.geospatial.GeoCoordinateOperations._
import com.here.platform.location.core.geospatial.{LineStringOperations, LineStrings}
import com.here.platform.location.core.graph.{PropertyMap, RangeBasedProperty}
import com.here.platform.location.dataloader.core.caching.CacheManager
import com.here.platform.location.dataloader.standalone.StandaloneCatalogFactory
import com.here.platform.location.inmemory.graph.Vertex
import com.here.platform.location.integration.herecommons.graph.Forward
import com.here.platform.location.integration.herecommons.graph.Vertices.directionOf
import com.here.platform.location.integration.heremapcontent.HereMapContentLayers
import com.here.platform.location.integration.heremapcontent.geospatial.Implicits._
import com.here.platform.location.integration.optimizedmap.geospatial.ProximitySearches
import com.here.platform.location.integration.optimizedmap.graph.PropertyMaps
import com.here.schema.geometry.v2.geometry.Point
import com.here.schema.rib.v2.advanced_navigation_attributes_partition.AdvancedNavigationAttributesPartition
import com.here.schema.rib.v2.common_attributes.SpeedLimitAttribute
import com.here.schema.rib.v2.navigation_attributes.LaneCountAttribute
import com.here.schema.rib.v2.navigation_attributes_partition.NavigationAttributesPartition
import com.here.schema.rib.v2.road_attributes.FunctionalClassAttribute
import com.here.schema.rib.v2.road_attributes.FunctionalClassAttribute.FunctionalClass
import com.here.schema.rib.v2.road_attributes_partition.RoadAttributesPartition

/** An example that shows how to compile Road attributes from HERE Map Content on the fly
  * and use them as properties of vertices from the `Optimized Map for Location Library`.
  */
object RoadAttributes extends App {

  case class MyTuple(vertex:Vertex, partitionId: String, segmentUri: String, direction: String,
                     start: Double, end: Double, fc: Int, lc: Int, sl: Int)

  val brandenburgerTor = Point(41.882, -87.635)
  val radiusInMeters = 1000.0

  val catalogFactory = new StandaloneCatalogFactory()

  val cacheManager = CacheManager.withLruCache()

  try {
    val optimizedMap = catalogFactory.create(OptimizedMapHRN, OptimizedMapVersion)
    val hereMapContent = optimizedMap.resolveDependency(HereMapContentHRN)


    val proximitySearch = ProximitySearches.from(optimizedMap, cacheManager)

    val verticesInRange = proximitySearch.search(brandenburgerTor, radiusInMeters).map(_.element)

    println(s"Number of vertices in range: ${verticesInRange.size}")

    // ----- Accessing RoadAttributes -----

    // define a RoadAttributesLoader to be passed to PropertyMap building
    val roadAttributesLoader = HereMapContentLayers.RoadAttributes.tileLoader(hereMapContent, cacheManager)

    // A partition resolver for RoadAttributes
    val roadPartitionResolver = HereMapContentLayers.RoadAttributes.partitionResolver(optimizedMap, cacheManager)

    // define which attribute field (e.g. functionalClass) to be extracted from FunctionalClassAttribute
    val functionalClassAccessor: AttributeAccessor[RoadAttributesPartition, FunctionalClass] = AttributeAccessors
      .forHereMapContentSegmentAnchor[RoadAttributesPartition, FunctionalClassAttribute,FunctionalClass](_.functionalClass,_.functionalClass)

    // build a FunctionalClass PropertyMap from defined parameters we have set above
    val functionalClassMap = PropertyMaps.rangeBasedProperty(optimizedMap, cacheManager, roadAttributesLoader, roadPartitionResolver, functionalClassAccessor)


    // ----- Accessing NavigationAttributes -----

    // define a NavigationAttributesLoader
    val navigationAttributesLoader = HereMapContentLayers.NavigationAttributes.tileLoader(hereMapContent, cacheManager)

    // A partition resolver for NavigationAttributes
    val navigationPartitionResolver = HereMapContentLayers.NavigationAttributes.partitionResolver(optimizedMap, cacheManager)

    // define which attribute field (e.g. value) to be extracted from SpeedLimitAttribute
    val speedLimitAccessorFC12 = AttributeAccessors.forHereMapContentSegmentAnchor[NavigationAttributesPartition, SpeedLimitAttribute, Int](_.speedLimit, _.value)

    // build a SpeedLimit PropertyMap for Functional Class 1, 2 roads
    val speedLimitMapFC12 = PropertyMaps.rangeBasedProperty(optimizedMap, cacheManager, navigationAttributesLoader, navigationPartitionResolver, speedLimitAccessorFC12)

    // define a LaneCountAttribute accessor
    val laneCountAccessor = AttributeAccessors.forHereMapContentSegmentAnchor[NavigationAttributesPartition, LaneCountAttribute, Int](_.throughLaneCount, _.laneCount)

    // build a LaneCount property map
    val laneCountMap = PropertyMaps.rangeBasedProperty(optimizedMap, cacheManager, navigationAttributesLoader, navigationPartitionResolver, laneCountAccessor)


    // ----- Accessing AdvancedNavigationAttributes -----

    // define a AdvancedNavigationAttributesLoader
    val advancedNavigationAttributesLoader = HereMapContentLayers.AdvancedNavigationAttributes.tileLoader(hereMapContent,cacheManager)

    // A partition resolver for AdvancedNavigationAttributes
    val advancedPartitionResolver = HereMapContentLayers.AdvancedNavigationAttributes.partitionResolver(optimizedMap,cacheManager)

    // define which attribute field (e.g. value) to be extracted from SpeedLimitAttribute
    val speedLimitAccessorFC345 = AttributeAccessors.forHereMapContentSegmentAnchor[AdvancedNavigationAttributesPartition, SpeedLimitAttribute, Int](_.speedLimit, _.value)

    // build a SpeedLimit PropertyMap for Functional Class 3, 4, 5 roads
    val speedLimitMapFC345 = PropertyMaps.rangeBasedProperty(optimizedMap,cacheManager,advancedNavigationAttributesLoader,advancedPartitionResolver,speedLimitAccessorFC345)


    // build a map of vertex to tileId, segmentId, direction information
    val optimizedMapToHereMapContent = PropertyMaps.vertexToHereMapContentReference(optimizedMap, cacheManager)

    // Filter vertices with LaneCount and SpeedLimit attributes available
    val filtered = verticesInRange.filter(v => laneCountMap(v).nonEmpty && (speedLimitMapFC12(v).nonEmpty || speedLimitMapFC345(v).nonEmpty))

    // loop over filted data
    val data = filtered.flatMap(vertex => {
      val fcs = functionalClassMap(vertex)
      val ref = optimizedMapToHereMapContent(vertex)
      val result = fcs.map(fc => {
        val start = if (fc.start < 0) 0.0 else fc.start
        val end = if (fc.end > 1) 1.0 else fc.end
        val offset = (start + end) / 2.0
        val laneCount = if (laneCountMap(vertex, offset).nonEmpty) laneCountMap(vertex, offset).get.value else 0
        // speedlimit needs to be accessed from two different propertyMap depending on FC
        val speedlimit = if (fc.value.value <= 2) {
          if (speedLimitMapFC12(vertex, offset).nonEmpty) speedLimitMapFC12(vertex, offset).get.value
          else 0
        }
        else {
          if (speedLimitMapFC345(vertex, offset).nonEmpty) speedLimitMapFC345(vertex, offset).get.value
          else 0
        }
        // features and labels can be constructed from the variables later, but we put them here for convenience.
        MyTuple(vertex, ref.partitionId.value, ref.segmentUri.value, ref.direction.toString, start, end,
          fc.value.value, laneCount, speedlimit)
      })
      // filter that laneCount and speedlimit have meaningful values
      //result.filter(t => t.lc> 0 && t.sl > 0)
      result
    }).toList

    //data.foreach(println)

    val geometryPropertyMap = PropertyMaps.geometry(optimizedMap, cacheManager)

    serializeToGeoJson(data, geometryPropertyMap)


  } finally {
    catalogFactory.terminate()
  }

  def yellowToRedGradient(speed: Float, min: Float = 0F, max: Float = 1F): Color = {
    require(min <= max, s"Min ($min) must be lower than max ($max)")
    val ratio = (max - speed) / (max - min)
    val byteValue = (math.min(math.max(ratio, 0F), 1F) * 255).toInt
    new RGB(255, byteValue, 0)
  }


  private def serializeToGeoJson[LS: LineStringOperations](mytuples: Iterable[MyTuple],
                                                            geometry: PropertyMap[Vertex, LS]): Unit = {

    import au.id.jazzy.play.geojson
    import au.id.jazzy.play.geojson._
    import com.here.platform.example.location.utils.Visualization._
    import com.here.platform.location.core.geospatial.GeoCoordinate._
    import play.api.libs.json._

    import scala.collection.immutable

    val lsOps = implicitly[LineStringOperations[LS]]
    import lsOps.PointGeoCoordinateOperations

    val segmentsAsFeatures = mytuples.flatMap(mt => {
      //println(mt.start, mt.end)
      val lineString = lsOps.points(geometry(mt.vertex)).map(_.toLocationGeoCoordinate)
      val nlc = if (mt.lc < 1) 1 else mt.lc
      val partialLine = LineStrings.pointForFraction(lineString, mt.start) +: lineString
        .dropWhile(pt => LineStrings.fractionForPoint(lineString, pt) <= mt.start)
        .takeWhile(pt => LineStrings.fractionForPoint(lineString, pt) < mt.end) :+ LineStrings
        .pointForFraction(lineString, mt.end)
      //val shift = Visualization.shiftNorthWest(if (directionOf(mt.vertex) == Forward) 2 else -2) _
      (1 to nlc).map(lc => {
        val shift = Visualization.shiftNorthWest(if (directionOf(mt.vertex) == Forward) lc else -lc) _
        val shiftedPartialLine = partialLine.map(shift)
        //val shiftedPartialLine = partialLine
        //val color = yellowToRedGradient(mt.sl.toFloat, 0, 100)
        val color = Visualization.redToYellowGradient(lc.toFloat, 0, 5)
        val width = 1
        Feature(geojson.LineString(shiftedPartialLine.to[immutable.Seq]),
          Some(Style(color, width)
            + ("color" -> JsString(color.value)) + ("width" -> JsString(width.toString))
            + ("tileId" -> JsString(mt.partitionId))
            + ("segmentId" -> JsString(mt.segmentUri))
            + ("direction" -> JsString(mt.direction))
            + ("offset_start" -> JsString(mt.start.toString))
            + ("offset_end" -> JsString(mt.end.toString))
            + ("speedLimit" -> JsString(mt.sl.toString))
            + ("functionalClass" -> JsString(mt.fc.toString))
            + ("laneCount" -> JsString(mt.lc.toString))
          )
        )
      })
    })
      .to[immutable.Seq]
    val json = Json.toJson(FeatureCollection(segmentsAsFeatures))
    val path = FileNameHelper.exampleJsonFileFor(this).toPath
    Files.write(path, Json.prettyPrint(json).getBytes)
    System.out.println("\nA GeoJson representation of the result is available in " + path + "\n")
  }


}
