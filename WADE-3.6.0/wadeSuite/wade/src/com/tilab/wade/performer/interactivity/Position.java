/*****************************************************************
 WADE - Workflow and Agent Development Environment is a framework to develop 
 multi-agent systems able to execute tasks defined according to the workflow
 metaphor.
 Copyright (C) 2008 Telecom Italia S.p.A. 

 GNU Lesser General Public License

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation, 
 version 2.1 of the License. 

 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the
 Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 Boston, MA  02111-1307, USA.
 *****************************************************************/
package com.tilab.wade.performer.interactivity;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.tilab.wade.performer.interactivity.ConstraintException;

//#ANDROID_EXCLUDE_BEGIN
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
//#ANDROID_EXCLUDE_END
public class Position extends Map {
	private static final long serialVersionUID = 4467886152512749472L;

	private static final double INVALID_VALUE = Double.NEGATIVE_INFINITY;
	
	private double longitude, latitude, altitude, accuracy;
	private boolean valid;
	private Image positionMarker;

	protected Position() {
		// Do not remove, used by JAXB
	}
	
	public Position(String id, Image positionMarker, Image defaultMarker) {
		super(id, defaultMarker);
		
		this.positionMarker = positionMarker;
		this.latitude = this.longitude = this.altitude = this.accuracy = INVALID_VALUE;

		addConstraint(new PermittedValuesConstraint(new Object[] { Boolean.TRUE }));
	}

	public Image getPositionMarker() {
		return positionMarker;
	}

	public void setPositionMarker(Image positionMarker) {
		this.positionMarker = positionMarker;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
		
		checkValid();
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;

		checkValid();
	}

	public double getAltitude() {
		return altitude;
	}

	public void setAltitude(double altitude) {
		this.altitude = altitude;

		checkValid();
	}

	public double getAccuracy() {
		return accuracy;
	}

	public void setAccuracy(double accuracy) {
		this.accuracy = accuracy;
		
		checkValid();
	}
	
	protected void checkValid() {
		valid = (longitude != INVALID_VALUE && latitude != INVALID_VALUE && altitude != INVALID_VALUE && accuracy != INVALID_VALUE);
	}

    @Override
    public void doValidate() throws ConstraintException {
            validateConstraints(valid);
    }

	@Override
	public void stamp() {
		super.stamp();

		positionMarker = null;
	}
	
	@Override
	protected Object getCacheValue() {
		//positionMarker is set to null in stamp() so it must not be cached ( the user can't modify it)	
		return new Object[] {super.getCacheValue(), longitude, latitude, altitude, accuracy, valid};
	}

	@Override
	protected void setCacheValue(Object value) {
		Object[] cache = (Object[])value;
		super.setCacheValue(cache[0]);
		longitude = (Double)cache[1];
		latitude = (Double)cache[2];
		altitude = (Double)cache[3];
		accuracy = (Double)cache[4];
		valid = (Boolean)cache[5];
		
	}
}
