import jade.core.Agent;

import java.util.Random;

import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class PersoneAgent extends Agent {

	private boolean status;

	private AID[] restaurants;
	
	private int indexRandomRestaurant;

	protected void setup() {
		
		System.out.println("Persone : "+getAID().getLocalName()+" is ready.");
		Random rand = new Random(); 
		addBehaviour(new TickerBehaviour(this, rand.nextInt(30000) + 10000) {
			protected void onTick() {
				
				System.out.println("\nPersone : "+getAID().getLocalName()+" Trying to reserve a place.");

				DFAgentDescription template = new DFAgentDescription();
				ServiceDescription sd = new ServiceDescription();
				sd.setType("reservation-restaurant");
				template.addServices(sd);
				try {
					DFAgentDescription[] result = DFService.search(myAgent, template); 
					System.out.println("Persone : "+getAID().getLocalName()+" found the following restaurant :");

					restaurants = new AID[result.length];
					for (int i = 0; i < result.length; ++i) {
						restaurants[i] = result[i].getName();
						System.out.println("\t" + restaurants[i].getLocalName());
						}
				}
				catch (FIPAException fe) {
					fe.printStackTrace();
				}

				myAgent.addBehaviour(new CallForReservation());

			}
		} );
}

	protected void takeDown() {
		System.out.println("Persone : "+getAID().getLocalName()+" terminating.\n");
	}


	private class CallForReservation extends Behaviour {

		private MessageTemplate mt;
		private int step = 0;

		public void action() {
			
			switch (step) {
			case 0:

				ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
				Random rand = new Random(); 
				indexRandomRestaurant = rand.nextInt(restaurants.length);
				System.out.println("Personne : " + getAID().getLocalName() + " chose the restaurant : " + restaurants[indexRandomRestaurant].getLocalName());
				cfp.addReceiver(restaurants[indexRandomRestaurant]);
				cfp.setContent("1");
				cfp.setReplyWith("cfp"+System.currentTimeMillis());
				myAgent.send(cfp);

				mt = MessageTemplate.MatchInReplyTo(cfp.getReplyWith());
				step = 1;

				break;
			case 1:

				ACLMessage reply = myAgent.receive(mt);
				if (reply != null) {
					if (reply.getPerformative() == ACLMessage.INFORM) {
						System.out.println("Persone : "+getAID().getLocalName()+" successfully reserved a place from "+reply.getSender().getLocalName());
						myAgent.doDelete();
					}
					else {
						System.out.println("Persone : "+getAID().getLocalName()+" failed to reserve from "+reply.getSender().getLocalName());
					}
					step = 2;

				}
				else {
					block();
				}

				break;
			}        
		}

		public boolean done() {
			return (step == 2 );
		}
	}
	
}