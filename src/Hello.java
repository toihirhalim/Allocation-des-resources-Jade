import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

public class Hello {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		int peopleCount = 10;
		String [] people = {"Halim", "Fati", "Siham", "Yasser", "Amina", "Omar", "Rachida", "Ali"};
		String [][] restaurants = {{"Caniada", "2"}, {"Acordeon", "1"}, {"Epsilon", "1"}};

		try {
			ProfileImpl p = new ProfileImpl();
		    p.setParameter(Profile.MAIN_HOST, "localhost");
		    p.setParameter(Profile.GUI, "true");
	
		    ContainerController cc = Runtime.instance().createMainContainer(p);
	
		    AgentController [] ACPeople = new AgentController[people.length] ;
		    AgentController [] ACRestaurants = new AgentController[restaurants.length] ;
		    
		    for(int i = 0; i < ACPeople.length; i++) {
		    	ACPeople[i] = cc.createNewAgent(people[i], "PersoneAgent", new Object[] { });
		    	ACPeople[i].start();
		    }
		    
		    for(int i = 0; i < ACRestaurants.length; i++) {
		    	ACRestaurants[i] = cc.createNewAgent(restaurants[i][0], "RestaurantAgent", new Object[] {restaurants[i][1]});
		    	ACRestaurants[i].start();
		    }
		    		
		} catch (StaleProxyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
	}

}
