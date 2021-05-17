import java.util.ArrayList;
import java.util.List;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

public class Hello {
	static String [] people = {"Halim", "Fati", "Siham", "Yasser", "Amina", "Omar", "Rachida", "Ali"};
	static String [][] restaurants = {{"Caniada", "1"}, {"Acordeon", "2"}, {"Epsilon", "5"}, {"Galaxy", "7"}};
	static List<int[]> summary = new ArrayList();
	
	public static void main(String[] args) {
		
		try {
			ProfileImpl p = new ProfileImpl();
		    p.setParameter(Profile.MAIN_HOST, "localhost");
		    p.setParameter(Profile.LOCAL_PORT, "12344");
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
	
	public static void addSummary(String person, String restaurant, int nbCalls) {
		int [] sum = new int[3];
		sum[0] = getIndexPerson(person);
		sum[1] = getIndexRestaurant(restaurant);
		sum[2] = nbCalls;
		
		summary.add(sum);
		
		if(summary.size() >= people.length) {
			printSummary();
		}
	}
	static int getIndexPerson(String person) {
		for(int i = 0; i < people.length; i++) {
			if(people[i].equals(person)) {
				return i;
			}
		}
		return  -1;
	}
	static int getIndexRestaurant(String restaurant) {
		for(int i = 0; i < restaurants.length; i++) {
			if(restaurants[i][0].equals(restaurant)) {
				return i;
			}
		}
		return  -1;
	}
	static void printSummary() {
		int sumCalls = 0;
		System.out.println("\n\t\t\tSummary");
		line();
		System.out.println("| Personne\t| Restaurant\t| Nombre d appels |");
		line();
		for(int[] arr : summary) {
			if(people[arr[0]].length() <= 5) {
				System.out.println("| " + people[arr[0]] + "\t\t| " + restaurants[arr[1]][0] + "\t| " + arr[2] + "\t\t  |");
			}else {
				System.out.println("| " + people[arr[0]] + "\t| " + restaurants[arr[1]][0] + "\t| " + arr[2] + "\t\t  |");
			}
			line();
			sumCalls += arr[2];
		}
		System.out.println("Nombre moyen d appels : " + (double) sumCalls / summary.size() + " calls \n");
	}
	static void line() {
		System.out.print(" ");
		for(int i = 0; i < 49; i++) {
			System.out.print("-");
		}
		System.out.println();
	}
}
