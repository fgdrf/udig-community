package eu.udig.community.solarterminator;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.util.Calendar;
import java.util.Collections;

import net.refractions.udig.mapgraphic.MapGraphic;
import net.refractions.udig.mapgraphic.MapGraphicContext;
import net.refractions.udig.ui.graphics.ViewportGraphics;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.vividsolutions.jts.awt.PolygonShape;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;

public class DayNightTerminatorGraphic implements MapGraphic {

    private static final Color colorFill = new Color(Color.lightGray.getRed(), Color.lightGray.getGreen(),
            Color.lightGray.getBlue(), 128);
    

    @Override
    public void draw(MapGraphicContext context) {
        ViewportGraphics graphics = context.getGraphics();
        
        CoordinateReferenceSystem destinationCRS = context.getCRS();
        CoordinateReferenceSystem sourceCRS = null;
        MathTransform transformMapCRStoWGS84 = null;
        MathTransform transformWGS84toMapCrs = null;
        boolean ignoreTransform = false;
        try {
            sourceCRS = CRS.decode("EPSG:4326");
            
            if (sourceCRS.equals(destinationCRS)) {
                ignoreTransform = true;
            } else {
                transformWGS84toMapCrs = CRS.findMathTransform(sourceCRS, destinationCRS, true);
            
                transformMapCRStoWGS84 = transformWGS84toMapCrs.inverse();
                if (transformWGS84toMapCrs == null || transformMapCRStoWGS84 == null) {
                    return;
                }
            }
        } catch (Exception e) {
            return;
        }

        
        
//        ReferencedEnvelope bounds = context.getMap().getBounds(new NullProgressMonitor());
        GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING_SINGLE));

        // use local and TimeZone settings
        Calendar calendar = Calendar.getInstance();
        // TODO get the date from configuration
        // calendar.setTime(date);
        
        DayNightTerminator terminator = new DayNightTerminator(calendar);
        
        Coordinate[] polygon2 = terminator.getPolygon();
        Geometry terminatorGeometry = GEOMETRY_FACTORY.createPolygon(GEOMETRY_FACTORY.createLinearRing(polygon2),null);
        
        if (!ignoreTransform) {
            terminatorGeometry = transform(transformWGS84toMapCrs, terminatorGeometry);
        }
        
        Shape s = new PolygonShape(terminatorGeometry.getCoordinates(), Collections.emptyList());
        AffineTransform worldToScreenTransform = context.worldToScreenTransform();
        s = worldToScreenTransform.createTransformedShape(s);
        graphics.setColor(colorFill);
        graphics.fill(s);

        
    }

    private Geometry transform(MathTransform transform,
            Geometry geometry) {
        try {
            return JTS.transform(geometry, transform);
        } catch (Exception e) {
            // ignore
        }
        return null;
    }
}
