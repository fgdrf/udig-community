/* Spatial Operations & Editing Tools for uDig
 * 
 * Axios Engineering under a funding contract with: 
 *      Diputación Foral de Gipuzkoa, Ordenación Territorial 
 *
 *      http://b5m.gipuzkoa.net
 *      http://www.axios.es 
 *
 * (C) 2006, Diputación Foral de Gipuzkoa, Ordenación Territorial (DFG-OT). 
 * DFG-OT agrees to licence under Lesser General Public License (LGPL).
 * 
 * You can redistribute it and/or modify it under the terms of the 
 * GNU Lesser General Public License as published by the Free Software 
 * Foundation; version 2.1 of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */
package es.axios.udig.ui.commons.util;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.refractions.udig.catalog.IGeoResource;
import net.refractions.udig.project.ILayer;
import net.refractions.udig.project.IMap;
import net.refractions.udig.project.render.IViewportModel;
import net.refractions.udig.ui.ProgressManager;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.geotools.data.DataStore;
import org.geotools.data.DefaultQuery;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;

import es.axios.udig.ui.commons.internal.i18n.Messages;

/**
 * Layer Utilities
 * <p>
 * Commons operations to get and set layer components.
 * </p>
 * 
 * @author Mauricio Pazos (www.axios.es)
 * @author Gabriel Roldan (www.axios.es)
 * @since 1.1.0
 */
public final class LayerUtil {

    private LayerUtil() {
        // util class
    }

    /**
     * Creates a Query for the selected features of <code>layer</code> or all the features if it
     * has no selection set and <code>fallbackToWholeLayer</code> is <code>true</code>.
     * 
     * @param layer
     * @return a Query for the selection
     */
    private static DefaultQuery getSelectionQuery( final ILayer layer ) {

        final boolean queryForSelectedFeatures = true;
        final Query selectionQuery = layer.getQuery(queryForSelectedFeatures);
        final DefaultQuery layerQuery = new DefaultQuery(selectionQuery);
        CoordinateReferenceSystem layerCrs = layer.getCRS();
        if (ILayer.UNKNOWN_CRS == layerCrs) {
            CoordinateReferenceSystem mapCRS = MapUtil.getCRS(layer.getMap());
            layerQuery.setCoordinateSystem(mapCRS);
        }

        Filter filter = selectionQuery.getFilter();
        // no selection? perform over the whole layer
        if (Filter.EXCLUDE.equals(filter)) {
            layerQuery.setFilter(Filter.INCLUDE);
        }
        return layerQuery;
    }

    /**
     * Compoutes the number of features selected. It will return all if any feature is selected
     * 
     * @param layer an <code>ILayer</code> that can resolve to <code>FeatureSource</code> for
     *        which to compute the number of selected features.
     * @return the number of features selected, if any, or the total number of features in the layer
     *         if no features are selected,
     * @throws IOException
     */
    public static int getCountOfSelectedFeatures( final ILayer layer ) throws IOException {

        return getCountOfSelectedFeatures(layer, Filter.INCLUDE);
    }

    /**
     * Computes the number of features selected. It will return all if any feature is selected.
     * 
     * @param layer
     * @param filter
     * @return the count of features in layer or Integer.MAX_VALUE if the feature collection has
     *         more than Integer.MAX_VALUE features
     * @throws IOException
     */
    public static int getCountOfSelectedFeatures( final ILayer layer, final Filter filter ) throws IOException {

        FeatureSource<SimpleFeatureType, SimpleFeature> source = layer.getResource(FeatureSource.class, null);
        assert source != null;

        final Query query = getSelectionQuery(layer);
        FeatureCollection<SimpleFeatureType, SimpleFeature> features = source.getFeatures(query);
        // if empty select all layer is computed (filter none)
        if (features.isEmpty()) {
            features = source.getFeatures(filter);
        }
        int count = GeoToolsUtils.computeCollectionSize(features);

        return count;
    }

    /**
     * Returns the features selected in layer.
     * 
     * @param layer the layer to obtain the selected features from
     * @param fallbackToWholeLayer wether to return the whole layer's features if no selection is
     *        set for the layer
     * @return FeatureCollection<SimpleFeatureType, SimpleFeature> holding the selection
     * @throws IOException if occurs getting the selection
     */
    public static FeatureCollection<SimpleFeatureType, SimpleFeature> getSelectedFeatures( final ILayer layer ) throws IOException {
        return getSelectedFeatures(layer, Filter.INCLUDE);
    }

    /**
     * Retruns all layer's features
     * 
     * @param layer
     * @return all features
     * @throws IOException
     */
    public static FeatureCollection<SimpleFeatureType, SimpleFeature> findAllFeatures( final ILayer layer ) throws IOException {

        return getSelectedFeatures(layer, Filter.INCLUDE);
    }

    /**
     * Returns the features selected in layer or all feature if there is not any selected feature
     * 
     * @param layer the layer to obtain the selected features from
     * @param fallbackToWholeLayer wether to return the whole layer's features if no selection is
     *        set for the layer
     * @param extraFilter non null Filter to append to the layer's selection filter. Use
     *        {@link Filter#NONE} instead of <code>null</code> to indicate no additional filter.
     * @return FeatureCollection<SimpleFeatureType, SimpleFeature> holding the selection
     * @throws IOException if occurs getting the selection
     */
    public static FeatureCollection<SimpleFeatureType, SimpleFeature> getSelectedFeatures( final ILayer layer,
                                                         final Filter extraFilter ) throws IOException {

        assert layer != null;
        assert extraFilter != null;

        FeatureSource<SimpleFeatureType, SimpleFeature> source = layer.getResource(FeatureSource.class, ProgressManager.instance()
                                                                                     .get());
        if (source == null) {
            return FeatureCollections.newCollection();
        }

        FeatureCollection<SimpleFeatureType, SimpleFeature> features;

        DefaultQuery selectionQuery = getSelectionQuery(layer);

        if (!Filter.INCLUDE.equals(extraFilter)) {
            Filter selectionFilter = selectionQuery.getFilter();
            FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
            
            Filter filter = ff.and( selectionFilter, extraFilter );
            selectionQuery.setFilter( filter );

            features = source.getFeatures(selectionQuery);
        } else {
            features = source.getFeatures();
        }

        return features;
    }

    /**
     * Traverses the <code>map</code>'s layers ensuring it does not already contains a layer
     * named <code>name</code> and returns a non existent layer name, either by returning
     * <code>name</code> itself, or by adding a number postfix to it.
     * 
     * @param map
     * @param name
     * @return a non already existent layer name in the map
     */
    public static String findNewLayerName( final IMap map, final String name ) {
        String newLayerName = name;
        Set<String> layerNames = new HashSet<String>();
        for( ILayer layer : map.getMapLayers() ) {
            layerNames.add(layer.getName());
        }
        int matches = 1;
        while( layerNames.contains(newLayerName) ) {
            matches++;
            newLayerName = name + matches;
        }
        return newLayerName;
    }

    /**
     * Returns a non existing type name in <code>ds</code> based on the proposed <code>name</code>.
     * 
     * @param existingLayer the layer from which to inspect its enclosing map and referred DataStore
     *        to avoid duplicated type names
     * @param name a proposed type name to base the returned name on
     * @return <code>name</code> if it don't already exists in the layer's map nor in its
     *         DataStore. A non existent type name in the data store with <code>name</code> as
     *         prefix otherwise.
     * @throws IOException
     */
    public static String findNewLayerName( final ILayer existingLayer, final String name ) throws IOException {
        String newLayerName = findNewLayerName(existingLayer.getMap(), name);

        IGeoResource resource = existingLayer.findGeoResource(FeatureSource.class);
        if (resource != null) {
            FeatureSource<SimpleFeatureType, SimpleFeature> source = resource.resolve(FeatureSource.class, new NullProgressMonitor());
            DataStore ds = (DataStore) source.getDataStore();
            String[] dsTypes = ds.getTypeNames();
            Set<String> typeNames = new HashSet<String>(Arrays.asList(dsTypes));
            int matches = 1;
            while( typeNames.contains(newLayerName) ) {
                matches++;
                newLayerName = name + matches;
            }
        }
        return newLayerName;
    }

    /**
     * Returns the layer's CRS ensuring it is not {@link ILayer#UNKNOWN_CRS}
     * <p>
     * If <code>sourceLayer.getCRS()</code> is {@link ILayer#UNKNOWN_CRS}, it means the
     * underlying FeatureSource<SimpleFeatureType, SimpleFeature> has no CRS defined, thus, for the sake of performing spatial
     * operations, the Map's CRS is returned instead.
     * </p>
     * 
     * @param sourceLayer
     * @return the layer's CRS of the Map's one of the layer's CRS is {@link ILayer#UNKNOWN_CRS}
     */
    public static CoordinateReferenceSystem getCrs( ILayer sourceLayer ) {
        assert sourceLayer != null;
        CoordinateReferenceSystem crs = sourceLayer.getCRS();
        if (ILayer.UNKNOWN_CRS == crs) {
            IMap map = sourceLayer.getMap();
            IViewportModel viewportModel = map.getViewportModel();
            crs = viewportModel.getCRS();
        }
        return crs;
    }

    /**
     * @param layer
     * @return the throw message
     */
    private static String buildThrowMessage( final ILayer layer ) {
        return MessageFormat.format(Messages.LayerUtil_CanNotResolveFeatureSource, layer.getName());
    }

    public static Coordinate mapToLayer( IMap map, ILayer layer, Coordinate coordInMapCrs ) {
        try {
            MathTransform toLayer = layer.mapToLayerTransform();
            double[] src = {coordInMapCrs.x, coordInMapCrs.y};
            double[] dst = new double[2];
            toLayer.transform(src, 0, dst, 0, 1);
            return new Coordinate(dst[0], dst[1]);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } catch (TransformException ex) {
            throw new RuntimeException(ex);
        }
    }

}
