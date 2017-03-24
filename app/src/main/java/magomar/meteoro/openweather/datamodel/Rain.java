
package magomar.meteoro.openweather.datamodel;

import com.google.gson.annotations.SerializedName;


public class Rain{
   	@SerializedName("3h")
   	private Number h3;

 	public Number getH3(){
		return this.h3;
	}
	public void setH3(Number h3){
		this.h3 = h3;
	}
}
