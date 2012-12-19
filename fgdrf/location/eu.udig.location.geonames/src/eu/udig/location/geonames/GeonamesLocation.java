package eu.udig.location.geonames;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.geonames.Toponym;
import org.geonames.ToponymSearchCriteria;
import org.geonames.ToponymSearchResult;
import org.geonames.WebService;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import eu.udig.location.Location;

public class GeonamesLocation implements Location {

	/**
	 * TODO REVIEW : is there any convenience feature-type for geo-location objects with description attribute like name 
	 * @param keys
	 * @return
	 */
	static private SimpleFeatureType createAddressType(List<String> keys) {
		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
		builder.setName("Address");
		for (String key : keys) {
			if ("long".equals(key) || "lat".equals(key))continue; //$NON-NLS-1$ //$NON-NLS-2$
			builder.add(key, String.class);
		}
		String geometryAtt = "location";
		builder.add(geometryAtt, Point.class, DefaultGeographicCRS.WGS84); //$NON-NLS-1$
		builder.setDefaultGeometry(geometryAtt);

		try {
			return builder.buildFeatureType(); //$NON-NLS-1$
		} catch (Throwable e) {
			return null;
		}
	}

	@Override
	public List<SimpleFeature> search(String text, Envelope bbox,
			IProgressMonitor monitor) throws IOException {
		GeometryFactory geometryFactory = new GeometryFactory();
		List<SimpleFeature> result = new ArrayList<SimpleFeature>();
		try {
		ToponymSearchCriteria searchCriteria = new ToponymSearchCriteria();
		searchCriteria.setQ(text);
// TODO REVIEW : set a maximum of result to be fetched from query
		searchCriteria.setMaxRows(100);
		
			ToponymSearchResult searchResult = WebService.search(searchCriteria);
			
			SimpleFeatureType ADDRESS = null;
			for (Toponym toponym : searchResult.getToponyms()) {
				if (ADDRESS == null) {
					ADDRESS = createAddressType(Collections.singletonList(new String("name")));
				}
				Point point = geometryFactory.createPoint(new Coordinate(
						toponym.getLongitude(), toponym.getLatitude()));

				result.add(SimpleFeatureBuilder.build(ADDRESS, new Object[] {
						toponym.getName(), point}, null));
			}
		} catch (Exception e) {
			throw new IOException(e);
		}

		// filter the result by bbox
		if (bbox != null) {
			result = filter(result, bbox);
		}
		return result;
	}
	
	private List<SimpleFeature> filter(List<SimpleFeature> features, Envelope bbox) {
		List<SimpleFeature> filtered = new ArrayList<SimpleFeature>();
		for (SimpleFeature feature : features) {
			if (bbox.intersects(((Point)feature.getDefaultGeometry()).getCoordinate())) {
				filtered.add(feature);
			}
		}
		return filtered;
	}

	public static void main(String[] args) {
		ToponymSearchCriteria searchCriteria = new ToponymSearchCriteria();
		searchCriteria.setQ("Hamburg");
		searchCriteria.setMaxRows(100);
		try {
			ToponymSearchResult searchResult = WebService
					.search(searchCriteria);
			for (Toponym toponym : searchResult.getToponyms()) {
				 System.out
				 .println(toponym.getName() +
				 " ("+toponym.getCountryName()+"): " + toponym.getLatitude() +
				 " / " + toponym.getLongitude());
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getServiceName() {
		return WebService.getGeoNamesServer();
	}
}
