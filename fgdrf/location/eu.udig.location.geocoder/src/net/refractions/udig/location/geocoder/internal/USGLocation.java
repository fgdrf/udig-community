/* uDig - User Friendly Desktop Internet GIS client
 * http://udig.refractions.net
 * (C) 2004, Refractions Research Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation;
 * version 2.1 of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */
package net.refractions.udig.location.geocoder.internal;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;

import net.refractions.udig.location.Location;

import org.apache.xmlrpc.XmlRpcException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.geotools.data.ServiceInfo;
import org.geotools.feature.NameImpl;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Envelope;

/**
 * Find a location, using the USG service.
 *  
 * @author Jody Garnett
 * @since 1.0.0
 */
public class USGLocation implements Location {

    public List<SimpleFeature> search( String pattern, Envelope bbox, IProgressMonitor monitor )
            throws IOException {
        AddressSeeker seek = new AddressSeeker();
        List<SimpleFeature> stuff;
        try {
            stuff = seek.geocode( pattern );
        } catch (XmlRpcException e) {
            throw new IOException(e);
        }        
        return stuff;
    }

	@Override
	public String getServiceName() {
		return new String("http://geocoder.us");
	}
}
