import java.util.Random;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;


public class RestaurantAgent extends Agent {

	private int C;

	private int Ct;
	
	protected void setup() {

		Object[] args = getArguments();
		
		if (args != null && args.length > 0) {

			C = Integer.parseInt(args[0].toString());
			
			Ct=C;
	
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
	
			System.out.println("Restaurant : "+getAID().getLocalName()+" with capacity "+C+" is ready.");

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

				int requestPlaces =Integer.parseInt(msg.getContent());
				ACLMessage reply = msg.createReply();
				
				System.out.println("Restaurant : " + myAgent.getLocalName() + " recieved a call from : " + msg.getSender().getLocalName());

				if (requestPlaces <= Ct) {
					Ct-=requestPlaces;
					reply.setPerformative(ACLMessage.INFORM);
					System.out.println("Restaurant : "+msg.getSender().getLocalName()+" reserved "+requestPlaces+" places in "+myAgent.getLocalName());
					System.out.println("Restaurant :"+myAgent.getLocalName()+" has "+Ct+" places disponible ");
					if(Ct==0) {
						System.out.println("Restaurant : "+myAgent.getLocalName()+" is fully occupied.");
						//myAgent.doDelete();
					}
						
				}
				else {
					reply.setPerformative(ACLMessage.FAILURE);
					System.out.println("Restaurant : "+myAgent.getLocalName()+" no places for "+msg.getSender().getLocalName());
				}
				myAgent.send(reply);
			}
			else {
				block();
			}
		}
	} 
}