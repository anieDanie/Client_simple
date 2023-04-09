import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class Client {

    private Socket socket;
    static ObjectInputStream fromServer;
    static ObjectOutputStream toServer;

    private static String session;
    private static List<Course> listeFiltreeTrieeSigle;
    private static String messageConfirmation;

    // Constructeur: initialise la connexion et les flux d'entrée et de sortie des données sérialisées
    public Client(){
        try {
            this.socket = new Socket("localhost", 1337);
            //System.out.println("Le client se connecte au serveur...");
            toServer = new ObjectOutputStream(socket.getOutputStream());
            fromServer = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Fermer les ressources utilisées pour la connexion et les échanges de données client-serveur
    private void disconnect(){
        try {
            this.fromServer.close();
            this.toServer.close();
            this.socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Programme principal
    public static void main(String[] args) {

        System.out.println(" *** Bienvenue sur le portail d'inscription de l'UDEM *** ");

        afficherCours();

        selectionnerAction();

    }


    // Obtenir la valeur de l'option saisie par utilisateur
    private static int obtenirChoix(){

        Scanner sc = new Scanner(System.in);
        System.out.print("Choix: ");
        int choix = sc.nextInt();

        return choix;
    }

    // Afficher les options de sélection d'une session (et stocker la valeur dans la variable session)
    private static void selectionnerSession() {
        System.out.println("Veuillez choisir la session pour laquelle vous voulez consulter la liste de cours: ");
        System.out.println("1. Automne");
        System.out.println("2. Hiver");
        System.out.println("3. Ete");
        int choix = obtenirChoix();

        switch(choix){
            case 1:
                session = "Automne";
                break;
            case 2:
                session = "Hiver";
                break;
            case 3:
                session = "Ete";
                break;
            default: System.out.println("Option non disponible");
        }

    }

    // Transmettre la requête textuelle au serveur pour obtenir la liste de cours pour une session donnée
    // et désérialiser la liste reçue (pour la stocker dans la variable listeFiltreeTrieeSigle)
    private static void charger(){

        // Client se connecte au serveur
        Client c = new Client();

        // Passer la commande "CHARGER" et recevoir la liste de cours pour une session donnée du serveur
        String command_load = "CHARGER " + session;

        try {
            toServer.writeObject(command_load);
            toServer.flush();

            listeFiltreeTrieeSigle = (List<Course>) fromServer.readObject();

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        // Client se déconnecte du serveur après le traitement de la requête

        c.disconnect();

    }

    // Afficher la liste des cours pour une session donnée en console
    private static void afficherCours(){
        selectionnerSession();
        charger();

        System.out.println("Les cours offerts pour la session d'" + String.format("%s",session.toLowerCase())+ " sont:");

        for (int i = 0; i < listeFiltreeTrieeSigle.size(); i++){
            System.out.println(i+1 + ". " + listeFiltreeTrieeSigle.get(i).getCode()
                    + "\t" + listeFiltreeTrieeSigle.get(i).getName());
        }

    }

    // Transmettre la requête textuelle pour l'inscription à un cours et désérialiser la réponse
    // transmise par le serveur (pour la stocker dans la variable messageConfirmation)
    private static void inscrire(RegistrationForm rf){

        //Client se connecte au serveur
        Client c = new Client();

        //Passer la commande "INSCRIRE" et recevoir un message de confirmation du serveur
        String command_Register = "INSCRIRE ";

        try {
            toServer.writeObject(command_Register);
            toServer.flush();

            toServer.writeObject(rf);
            toServer.flush();

            messageConfirmation = (String) fromServer.readObject();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        //Client se déconnecte du serveur après le traitement de la requête
        c.disconnect();
    }

    // Afficher les options d'actions possibles et appeler les méthodes de traitement pertinentes
    private static void selectionnerAction() {
        System.out.println("Que souhaitez-vous faire ?");
        System.out.println("1. Consulter les cours offerts pour une autre session");
        System.out.println("2. Vous inscrire à un cours pour la session d'" + String.format("%s", session.toLowerCase()) );

        int choix = obtenirChoix();

        // il faudrait une boucle while ici, avec une condition de sortie, pour faire l'une ou l'autre ce des 2 actions sans interruption

        if (choix == 1) {
            afficherCours();
            selectionnerAction();
        }
        else
            //inscrire();
            traiterInscription();

    }

    // Obtenir les données saisies pour la création d'un formulaire d'inscription et valider l'existence du cours demandé
    private static RegistrationForm creerRegistrationForm(){
        String prenom, nom, email, matricule, codeCours;
        String nomCours = null;

        Scanner sc = new Scanner(System.in);
        System.out.print("\nVeuillez saisir votre prénom:");
        prenom = sc.nextLine();
        System.out.print("Veuillez saisir votre nom:");
        nom = sc.nextLine();
        System.out.print("Veuillez saisir votre email:");
        email = sc.nextLine();
        System.out.print("Veuillez saisir votre matricule:");
        matricule = sc.nextLine();
        System.out.print("Veuillez saisir le code du cours:");
        codeCours = sc.nextLine();

        int indexCours = Collections.binarySearch(listeFiltreeTrieeSigle,
                new Course(null,codeCours,null),
                (c1,c2) -> ((Course) c1).getCode().compareTo(c2.getCode()));

        try{
            nomCours = listeFiltreeTrieeSigle.get(indexCours).getName();

            Course c = new Course(nomCours,codeCours, session);

            RegistrationForm rf = new RegistrationForm (prenom, nom,email,matricule,c);

            return rf;

        } catch (IndexOutOfBoundsException e){
            System.out.println("Le code de ce cours n'est pas dans la liste !");
        }

        return null;
    }

    // Traiter l'inscription (si formulaire d'inscription créé est valide)
    private static void traiterInscription(){
        RegistrationForm rf = creerRegistrationForm();

        if ((rf != null)){
            inscrire(rf);
            System.out.println("\n" + messageConfirmation);
        }
        else{
            System.out.println("Saisissez de nouveau les informations requises du formulaire d'inscription.");
            traiterInscription();
        }
    }

}
