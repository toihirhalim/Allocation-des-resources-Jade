import java.util.Random;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;


public class RestaurantAgent extends Agent {

	private int capacity;
	private int availablePlaces;
	
	protected void setup() {

		Object[] args = getArguments();
		
		if (args != null && args.length > 0) {

			capacity = Integer.parseInt(args[0].toString());
			
			availablePlaces=capacity;
	
			DFAgentDescription dfd = new DFAgentDescription();
			dfd.setName(getAID());
			ServiceDescription sd = new ServiceDescription();
			sd.setType("reservation-restaurant");
			sd.setName("RESTAURANTION-SERVICE");
			dfd.addServices(sd);
			try {
				DFService.register(this, dfd);
			}
			catch (FIPAException fe) {
				fe.printStackTrace();
			}
	
			addBehaviour(new ReciveCallService());
	
			System.out.println("Restaurant : "+getAID().getLocalName()+" with capacity "+capacity+" is ready.");

		}
		else {
			System.out.println("Restaurant : error no argument enter for "+getAID().getLocalName());

			doDelete();
		}
	}

	protected void takeDown() {

		try {
			DFService.deregister(this);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		
		System.out.println("Restaurant : "+getAID().getLocalName()+" termining");
	}
	
	private class ReciveCallService extends CyclicBehaviour {
		
		public void action() {
			
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive(mt);
			
			if (msg != null) {
				try {
					Message messageContent = (Message) msg.getContentObject();
					
					if(messageContent.getType().equals("reserver-restaurant")) {
						int requestPlaces = Integer.parseInt(messageContent.getContent());
						ACLMessage reply = msg.createReply();
						
						System.out.println("Restaurant : " + myAgent.getLocalName() + " recieved a call from : " + msg.getSender().getLocalName());

						if (requestPlaces <= availablePlaces) {
							availablePlaces-=requestPlaces;
							reply.setPerformative(ACLMessage.INFORM);
							System.out.println("Restaurant : "+msg.getSender().getLocalName()+" reserved "+requestPlaces+" places in "+myAgent.getLocalName() + " (" + capacity +" places availables)");
							if(availablePlaces==0) {
								System.out.println("Restaurant : "+myAgent.getLocalName()+" is fully occupied.");
								//myAgent.doDelete();
							}
								
						}
						else {
							reply.setPerformative(ACLMessage.FAILURE);
							System.out.println("Restaurant : "+myAgent.getLocalName()+" no places for "+msg.getSender().getLocalName());
						}
						myAgent.send(reply);
					}else if (messageContent.getType().equals("leave-restaurant")) {
						int nbPlaces = Integer.parseInt(messageContent.getContent());
						
						availablePlaces = (availablePlaces + nbPlaces) <= capacity ? (availablePlaces + nbPlaces) : capacity;
						
						ACLMessage reply = msg.createReply();
						reply.setPerformative(ACLMessage.INFORM);
						System.out.println("Restaurant : "+msg.getSender().getLocalName()+" left restaurant : "+myAgent.getLocalName() + " (" + capacity +" places availables)");
						myAgent.send(reply);
					}
				} catch (UnreadableException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else {
				block();
			}
		}
	} 
}