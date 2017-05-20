import java.io.*;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Scanner;

/**
 * Created by James on 3/4/2017.
 */
public class Perceptron
{
    /**
     * Note to self to change the way this is implemented
     */

    public String[] inputVector = new String[10000];
    /**
     *  going to store the weight values for all of the words
     *  the String is the key, which is going to be the word
     *  the Integer is the weight value, that will range from 0-1 (subject to change)
     */
    public static HashMap weightVector = new HashMap<String,Integer>();
    public static String[] lettersToExclude = new String[10];
    public static int learningRate = 1;
    public static boolean evaluatingPositives;
    public static int foldCounter = 0;
    public static boolean includePunctuation  = false;
    public static String[][] folds = new String[2000][];
    public static String[] punctuation = {";",".",",",":","(",")","`"," ","|","\\"};

    public static double totalAccuracy;
    public static double totalPrecision;
    public static double totalRecall;

    public static void main(String args[]) {
        //String[] xInputVector = createXinputVector(selectedFile);                   // takes the selected file and makes an array of the words
        //addVectorToWeightHashMap(xInputVector);                                     // adds the input vector to the hash map of words : weights

        initializeWeightVectorToZeros("src\\txt_sentoken\\pos");
        initializeWeightVectorToZeros("src\\txt_sentoken\\neg");

        while(true) {
            System.out.println("would you like to include punctuation?(y/n)");
            Scanner in = new Scanner(System.in);
            String reponse = in.nextLine();
            if (reponse.contains("y")){
                includePunctuation = true;
                break;
            }
            else if (reponse.contains("n")){
                includePunctuation = false;
                break;
            }
            else{
                System.out.println("neither selected");
            }
        }
        System.out.println("processing...");

        /**
         * Now the folds are alternative between being positive and negative
         */
        createFoldMatrix("src\\txt_sentoken\\pos");
        foldCounter = 1;
        createFoldMatrix("src\\txt_sentoken\\neg");

        /**
         * now the folds matrix contains alternating positive and negative reviews with
         * fold 1: 0-399
         * fold 2: 400-799
         * fold 3: 800-1199
         * fold 4: 1200-1599
         * fold 5: 1600-1999
         */

        for(int i = 0; i < folds.length; i++) {
            /**
             * Now if the index of folds[] is even (0,2,4...) then it is a positive review
             *     if the index of folds[] is odd  (1,3,5...) then it is a negative review
             */
            if(i % 2 == 0) {
                updateWeightVector(folds[i], true);
            }
            else {
                updateWeightVector(folds[i], false);
            }
        }

        //displayAllWeights();
        System.out.print("#######################################");
        System.out.print("fold 1 results");
        testOnFold1(1);
        System.out.print("#######################################");
        System.out.print("fold 2 results");
        testOnFold1(2);
        System.out.print("#######################################");
        System.out.print("fold 3 results");
        testOnFold1(3);
        System.out.print("#######################################");
        System.out.print("fold 4 results");
        testOnFold1(4);
        System.out.print("#######################################");
        System.out.print("fold 5 results");
        testOnFold1(5);

        System.out.println("Average Results");
        System.out.println("average accuracy: " + (totalAccuracy/5));
        System.out.println("average precision: " + (totalPrecision/5));
        System.out.println("average recall: " + (totalRecall/5));
    }
    public static void testOnFold1(int fold){
        int weightSum = 0;
        int truePositive = 0;
        int falsePositive = 0;
        int trueNegative = 0;
        int falseNegative = 0;

        /**
         *
         */
        for(int i = 400*(fold-1); i < (400*fold); i++){
            for(int j = 0 ; j < folds[i].length; j++){
                if(folds[i][j] != null) {
                    weightSum += (Integer) weightVector.get(folds[i][j]);
                }
            }
            if(weightSum > 0){
                if(i % 2 == 0){
                    truePositive++;
                }
                else{
                    falsePositive++;
                }
            }
            else{
                if(i % 2 == 0){
                    falseNegative++;
                }
                else{
                    trueNegative++;
                }
            }
            //System.out.println(weightSum);
            weightSum = 0;
        }
        System.out.println("");
        System.out.println("True Positives: " + truePositive);
        System.out.println("False Positives: " + falsePositive);
        System.out.println("True Negative: " + trueNegative);
        System.out.println("False Negatives: " + falseNegative);

        double precisionPlus = (double)truePositive / (truePositive + falsePositive);
        double precisionMinus = (double)trueNegative / (trueNegative + falseNegative);
        double recallPlus = (double)truePositive / (truePositive + falseNegative);
        double recallMinus = (double)trueNegative / (trueNegative + falsePositive);
        double accuracy = (double)(truePositive + trueNegative)/(truePositive + trueNegative + falsePositive + falseNegative);
        double precision = (double)(precisionPlus + precisionMinus)/2;
        double recall    = (double)(recallPlus + recallMinus)/2;

        System.out.println("accuracy: " + accuracy);
        System.out.println("precision: " + precision);
        System.out.println("recall: " + recall);

        totalAccuracy += accuracy;
        totalPrecision += precision;
        totalRecall += recall;


        System.out.println("");

    }
    /**
     * Calls the addInputsToWeightVector Method
     */
    public static void updateWeightVector(String[] xInputVector, boolean positive) {
        // The weight vector alternates but starts with a positive review
        for(int i = 0; i < xInputVector.length; i++) {                              //prints all the values of that array
            if(xInputVector[i] == null){
                //System.out.println(i);  // i will be the amount of words that are in this xinputVector
                break;
            }
            int signumFunctionSum  = calculateSignumFunctionSum(xInputVector, positive);
            if(includePunctuation == false) {
                for (int j = 0; j < punctuation.length; j++) {
                    if (xInputVector[i].equalsIgnoreCase(punctuation[j])) {
                        i++;
                        break;
                    }
                }
            }
            updateWeightForXInputValue(xInputVector[i], signumFunctionSum, positive);
        }
    }
    public static int calculateSignumFunctionSum(String[] xInputVector, boolean positive) {
        int sum = 0;
        int  bias  = 10;
        for(int i = 0; i < xInputVector.length; i++) {                              //prints all the values of that array
            if(xInputVector[i] == null){
                break;
            }
            sum = sum + (Integer)weightVector.get(xInputVector[i]);
            // the weight of films is getting updated and so every movie containing the word film did not have an increase in weight because the sum was always going to be greater than one
            if((Integer)weightVector.get(xInputVector[i]) > 0){
                //System.out.println(xInputVector[i] + " has added to the sum");
            }

        }

        sum = sum + bias;
        if(sum > 0){
            return 1;
        }
        else if(sum < 0){
            return -1;
        }
        else {
            return 0;
        }

    }
    /**
     * If the word is already an input vector, then the weight just increases by 1 (subject to change)
     * If not then the weight is initiated as 1
     */
    public static void updateWeightForXInputValue(String key, int signumfunctionSum, boolean positive){
        int newWeight = 0;
        if(weightVector.containsKey(key)){

            if(positive == true) {
                //System.out.println("positive review");
                newWeight = (Integer)(weightVector.get(key)) + (learningRate)*(1 - (signumfunctionSum));
            }
            else {
                //System.out.println("negative review");
                newWeight = (Integer)(weightVector.get(key)) + (learningRate)*(-1 - (signumfunctionSum));
            }

            weightVector.put(key, newWeight);

        }
        else{
            weightVector.put(key, 0);
        }
//        if(key.equalsIgnoreCase("and")){
//            System.out.println(weightVector.get(key) + " " + newWeight + " " + signumfunctionSum + " " + positive);
//            System.out.println();
//
//        }
    }
    /**
     *  - This method calls a method on every single text file in a directory
     *  - For this case the directory is going to either be pos or neg
     */
    public static void createFoldMatrix(String path)
    {
        Path dir = FileSystems.getDefault().getPath(path);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path file: stream) {

                /**
                 * creates an array of words that are in the current file in the directory
                 */
                String[] xInputVector = createXinputVector(file.toFile());                   // takes the selected file and makes an array of the word
                folds[foldCounter] = xInputVector;
                foldCounter = foldCounter + 2;
                //updateWeightVector(xInputVector);
            }
        } catch (IOException | DirectoryIteratorException x) {
            System.err.println(x);
        }

    }
    public static void initializeWeightVectorToZeros(String path){
        Path dir = FileSystems.getDefault().getPath(path);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path file: stream) {
                //processSingleFile(file.toFile());
                //System.out.println(file.getFileName());
                String[] xInputVector = createXinputVector(file.toFile());                   // takes the selected file and makes an array of the words
                initializeVectorToWeightHashMap(xInputVector);
            }
        } catch (IOException | DirectoryIteratorException x) {
            System.err.println(x);
        }

    }
    public static void initializeVectorToWeightHashMap(String[] xInputVector){
        for(int i = 0; i < xInputVector.length; i++) {                              //prints all the values of that array
            if(xInputVector[i] == null){
                //System.out.println(i);  // i will be the amount of words that are in this xinputVector
                break;
            }
            weightVector.put(xInputVector[i],0);
        }
    }
    public static String[] createXinputVector(File selectedFile) {
        String words[] = new String[20];
        String[] xinputVector = new String[10000];
        int counter = 0;
        try {
            BufferedReader in = new BufferedReader(new FileReader(selectedFile));
            String text;
            while((text = in.readLine()) != null) {
                words = text.split(" ");
                for(int i = 0; i < words.length; i++) {
                    xinputVector[counter] = words[i];
                    counter++;
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("File Not Found for the BufferedReader");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return xinputVector;
    }
    public static void displayAllWeights() {
        System.out.println(weightVector.toString());        // prints all the words and their weights
    }
}
