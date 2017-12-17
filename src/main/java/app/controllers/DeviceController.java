package app.controllers;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.javalite.activeweb.AppController;
import org.javalite.activeweb.annotations.DELETE;
import org.javalite.activeweb.annotations.POST;

import app.models.Device;
import app.models.DeviceCron;
import app.models.User;

/* import google GSON*/

/**
 * @author Vitor "Pliavi" Silverio
 */
public class DeviceController extends AppController {
 
    public void index() {
        view("devices", Device.where("usuario_id").toMaps());
    }

    public void create() { /* it's empty just to create the route to view */ }

    @POST
    public void save() {
        User loggedUser = (User) session("user");
        
        Device device = new Device();
        device.fromMap(params1st());
        device.set("usuario_id", ((User) session("user")).get("id"));

        if(!device.save()){
            flash("error", "Algo deu errado, preencha todos os campos corretamente");
            flash("errors", device.errors());
            flash("params", params1st());
            redirect(DeviceController.class, "create");
            return;
        }

        flash("success", "Novo equipamento " + device.get("nome") + "adicionado!");
        redirect(DeviceController.class, "create");
    }

    public void edit() {
        view("device", Device.findById(getId()));

        render("create");
    }

    @POST
    public void update() {
        Device device = Device.findById(getId());
        Map<String, String[]> params = params();

        // for(Entry<String, String[]> param : params.entrySet()){
        //     device.set(param.getKey(), param.getValue());
        // }
        params.forEach((k,v) -> device.set(k,v));

        device.saveIt();
        setTimes((String) device.get("id"));
    }

    @DELETE
    public void delete() {
        Device.findById(getId()).delete();
        redirect();
    }

    /**
     * Receive the cron times to be set on unique device
     * @param crons The time to set a cron to swtich on the device
     */
    public void setTimes(String deviceId) {
        List<DeviceCron> crons = DeviceCron.where("device_id = ?", deviceId);
        List<String> cronsAssigned = params("crons");
        int existingCronsCount = crons.size();

        for(int i = 0; i < cronsAssigned.size(); i++) {
            if(i <= existingCronsCount) { 
                // Update existing ones
                DeviceCron cron = crons.get(i);

                cron.set("time", cronsAssigned.get(i));
                crons.set(i, cron);
            } else { 
                // Create new ones
                DeviceCron cron = new DeviceCron();

                cron.set("time", cronsAssigned.get(i));
                crons.add(cron);
            }

            crons.get(i).saveIt();
        }
    }

    /**
     * Verify if there is any device to be switched on
     * 
     * @return a list of devices to be switched on
     */
    public void verify() {
        User user = (User) session("user");
        List<Device> userDevices = user.getAll(Device.class);
        List<Integer> devices2SwitchOn = new ArrayList<>();
		List<Integer> devices2SwitchOff = new ArrayList<>();
		Map<String, List<Integer>> devices2Switch = new HashMap<>();

        Calendar calendar = Calendar.getInstance();
        int currentMillis = calendar.get(Calendar.MILLISECOND);
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

        for (Device device : userDevices) {
            List<DeviceCron> deviceCrons = device.getAll(DeviceCron.class);

            for (DeviceCron cron : deviceCrons) {
                try {
					calendar.setTime(dateFormat.parse((String) cron.get("time")));
                } catch (ParseException e) { e.printStackTrace(); }
                
                int cronMillis = calendar.get(Calendar.MILLISECOND);

                if (currentMillis >= cronMillis) {
                    this.switchState(device.get("id"), true);
                    break;
                }
            }

            if(((boolean) device.get("on"))) {
                devices2SwitchOn.add((int) device.get("id"));
            } else {
                devices2SwitchOff.add((int) device.get("id"));
            }
        }

		devices2Switch.put("ligar", devices2SwitchOn);
        devices2Switch.put("desligar", devices2SwitchOff);
        
        //String json = new Gson().toJson(devices2Switch);
        //respond(json).contentType("text/json").status(200);
    }


    @POST public void on()  { this.switchState(param("id"), true); redirect(DeviceController.class, "index"); }
    @POST public void off() { this.switchState(param("id"), false); redirect(DeviceController.class, "index"); }
    
    /*=== NON ROUTED FUNCTIONS ===*/
    /* Switch on and off */
    public void switchState(Object id, boolean state){
        Device device = Device.findById(Integer.parseInt((String) id));

        device.set("status", state);
        device.saveIt();
    }

}
