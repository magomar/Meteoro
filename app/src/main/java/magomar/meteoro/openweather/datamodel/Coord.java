
package magomar.meteoro.openweather.datamodel;

public class Coord{
   	private Number lat;
   	private Number lon;

 	public Number getLat(){
		return this.lat;
	}
	public void setLat(Number lat){
		this.lat = lat;
	}
 	public Number getLon(){
		return this.lon;
	}
	public void setLon(Number lon){
		this.lon = lon;
	}
}
