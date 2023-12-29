/*

Program plays Acey Duecy

*/

import java.net.Socket;
import java.util.Arrays; // arrays package 
import java.util.List; // list package
import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;

public class Acey {
    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;

    public Acey(String IpAddress, int IpPort) throws IOException {
        socket = new Socket(IpAddress, IpPort);
        dis = new DataInputStream(socket.getInputStream());
        dos = new DataOutputStream(socket.getOutputStream());
    }

    private void write(String s) throws IOException {
        dos.writeUTF(s);
        dos.flush();
    }

    private String read() throws IOException {
        return dis.readUTF();
    }

    public void start() throws IOException {
        while (true) {
            String command = read();

            if (command.startsWith("login")) { // login command
                handleLogin();
            } else if (command.startsWith("play")) { // play command
                handlePlay(command);
            } else if (command.startsWith("status")) { // status command
                handleStatus(command);
            } else if (command.startsWith("done")) { // done command
                handleDone(command);
                break;
            }
        }
    }

    private void handleLogin() throws IOException {
        write("koushalsmodi:SUN"); // github id and avatar
    }

    private void handlePlay(String command) throws IOException {
        // parse the command and determine your strategy for betting
        String[] parts = command.split(":"); // splitting with delimeter as a colon
        int pot = Integer.parseInt(parts[1]); // number of chips in pot
        int stack = Integer.parseInt(parts[2]); // number of chips in stack
        String card1 = parts[3]; // my first card
        String card2 = parts[4]; // my second card
        List<String> prevCards = Arrays.asList(Arrays.copyOfRange(parts, 6, parts.length)); // dealt cards

        int value1 = getCardValue(card1); // card 1 value
        int value2 = getCardValue(card2); // card 2 value

        String reply = "mid";

        int bet; // bet is an integer

        if (value1 == value2) { // if card 1 value is equal to card value
            int[] hits = getHits(prevCards, value1, 0, 0, true); // passing parameters into the getHits function

            // checking if chance of getting an equal card is higher than getting a low or
            // high
            if (hits[1] > hits[0] && hits[1] > hits[2]) {
                reply = "low";
                bet = 0; // to save from losing 3 times the money

            } else if (hits[0] > hits[2]) { // checking if chance of getting a low card is higher than high
                reply = "low";
                bet = (int) (stack * (hits[0] / (364 - prevCards.size()))); // probability, betting proportionally

            } else if (hits[0] < hits[2]) { // checking if chance of getting a high card is higher than low
                reply = "high";
                bet = (int) (stack * (hits[2] / (364 - prevCards.size()))); // probability, betting proportionally

            } else { // if all are equal, go for low, low equal hits = high hits, all probability
                     // same
                reply = "low";
                bet = (int) (stack * (hits[0] / (364 - prevCards.size()))); // probability, betting proportionally
            }
        } else if (Math.abs(value1 - value2) == 1) { // if cards are consecutive, bet 0 as no cards can be in between
            bet = 0;

        } else { // if cards are not equal and not consecutive, bet on the middle card

            int low = Math.min(value1, value2); // low is the minimum of the 2 cards
            int high = Math.max(value1, value2); // high is the maximum of the 2 cards
            int[] hits = getHits(prevCards, 0, low, high, false);
            bet = (int) (stack * (hits[0] / (364 - prevCards.size()))); // probability, betting proportionally
        }
        write(reply + ":" + Math.min(pot, bet)); // also ensuring that bet is not more than pot
    }

    private static int[] getHits(List<String> dealtCards, int value, int low, int high, boolean isEqual) {
        // clubs = [0,0,0,0,0,0,0,0,0,0,0,0,0] //
        // if 5, then since it is [2,3,4,5,6,7,8,9,10,11,12,13,14], nned to do 5-2, that
        // is, 3 to get to 5
        // diamonds = [0,0,0,0,0,0,0,0,0,0,0,0,0]
        // hearts = [0,0,0,0,0,0,0,0,0,0,0,0,0]
        // spades = [0,0,0,0,0,0,0,0,0,0,0,0,0]

        // array of size 13, all 0s for each suit
        int[] clubs = new int[13];
        int[] diamonds = new int[13];
        int[] hearts = new int[13];
        int[] spades = new int[13];

        // each value in the suit has a frequency of 7 at the beginning, since we have a
        // megadeck
        for (int i = 0; i < 13; i++) {
            clubs[i] = 7;
            diamonds[i] = 7;
            hearts[i] = 7;
            spades[i] = 7;
        }

        for (String card : dealtCards) { // card will reference value of each element in the dealtCards
            int val = getCardValue(card); // val is the integer value obtained by reading the card

            // if the value of the card is 5, then it is at index "5-2" in the clubs array
            // and then decrement the counter by 1
            // if the value of the card is n, then it is at index "n-2" in the clubs array

            if (card.endsWith("C")) {
                clubs[val - 2]--;
            } else if (card.endsWith("D")) {
                diamonds[val - 2]--;
            } else if (card.endsWith("H")) {
                hearts[val - 2]--;
            } else if (card.endsWith("S")) {
                spades[val - 2]--;
            }
        }

        /*
         * In the case where the two cards that are dealt are equal
         * we need to find the number of cards with value less than
         * the value of the given two cards, number of cards with the same value as the
         * given cards value,
         * and the number of cards with value greater than the given cards value.
         */

        if (isEqual) {
            int lowHits = 0; // number of cards avaliable in the deck that have lower value than the card we
                             // received
            int highHits = 0; // number of cards avaliable in the deck that have higher value than the card we
                              // received value
            int equalHits = 0; // number of cards in the deck that have equal values

            for (int i = 0; i < 13; i++) {
                if (i < value - 2) {
                    // lowHits is the number of remaining cards in the mega deck that have value
                    // less than the given cards value
                    lowHits += clubs[i] + diamonds[i] + hearts[i] + spades[i];
                } else if (i > value - 2) {
                    // highHits is the number of remaining cards in the mega deck that have value
                    // greater than the given cards value
                    highHits += clubs[i] + diamonds[i] + hearts[i] + spades[i];
                } else {
                    // equalHits is the number of remaining cards in the mega deck that have value
                    // greater than the given cards value
                    equalHits += clubs[i] + diamonds[i] + hearts[i] + spades[i];
                }
            }

            return new int[] { lowHits, equalHits, highHits }; // 3 elements
        }

        // if we are looking in case of not equal cards, we need to check for hits in
        // between low and high.
        int hits = 0;
        for (int i = 0; i < 13; i++) {
            if (i > low - 2 && i < high - 2) {
                hits += clubs[i] + diamonds[i] + hearts[i] + spades[i]; // frequency of values of cards that are between
                                                                        // low and high

            }
        }

        return new int[] { hits }; // 1 element

    }

    private static int getCardValue(String card) {
        char cardValue = card.charAt(0); // character at index 0
        if (Character.isDigit(cardValue)) { // it the character is a digit
            if (card.startsWith("10")) { // and starts with 10
                return 10; // return 10
            }
            return cardValue - '0'; // for example, cardvalue = '5' has value 53, and 'O' has 48, so subtracting
                                    // ASCII numbers gives 5
        } else if (cardValue == 'J') {
            return 11; // return 11 if Jack
        } else if (cardValue == 'Q') {
            return 12; // return 12 if Queen
        } else if (cardValue == 'K') {
            return 13; // return 13 if King
        } else if (cardValue == 'A') {
            return 14; // return 14 if Ace
        }
        return 0; // if invalid card
    }

    private void handleStatus(String command) {
        // no reply needed
    }

    private void handleDone(String command) {
        System.out.println(command.split(":")[1]); // done command
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Please provide IP Address and IP Port as arguments."); // need both ipaddress and ip
                                                                                       // port
            System.exit(1); // and exit
        }
        try {
            Acey acey = new Acey(args[0], Integer.parseInt(args[1])); // take in input: ipaddress and ip port
            acey.start(); // call the start function
        } catch (IOException e) { // exception handler
            e.printStackTrace();
        }
    }
}