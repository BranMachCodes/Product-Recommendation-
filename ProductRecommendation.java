import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class ProductRecommendation {



// Load data from the CSV file
private static Map<String, List<String>> loadData(String filepath) throws IOException {
    Map<String, List<String>> customerPurchases = new HashMap<>();
    try (BufferedReader br = new BufferedReader(new FileReader(filepath))) {
        String line = br.readLine(); // Skip header
        while ((line = br.readLine()) != null) {
            String[] parts = line.split(",");
            if (parts.length < 3) continue; // Skip invalid rows
            String customer = parts[0].trim(); // Member_number
            String item = parts[2].trim();    // itemDescription
            customerPurchases.computeIfAbsent(customer, k -> new ArrayList<>()).add(item);
        }
    }
    return customerPurchases;
}


private static Map<String, Map<String, Double>> buildRecommendationModel(Map<String, List<String>> customerPurchases) {
    Map<String, Map<String, Integer>> rawCounts = new HashMap<>();
    Map<String, Integer> itemFrequency = new HashMap<>();

    // Count co-occurrences and item frequencies
    for (List<String> items : customerPurchases.values()) {
        for (int i = 0; i < items.size(); i++) {
            String item1 = items.get(i);
            itemFrequency.merge(item1, 1, Integer::sum);
            for (int j = i + 1; j < items.size(); j++) {
                String item2 = items.get(j);
                if (!item1.equals(item2)) {
                    rawCounts.computeIfAbsent(item1, k -> new HashMap<>())
                             .merge(item2, 1, Integer::sum);
                    rawCounts.computeIfAbsent(item2, k -> new HashMap<>())
                             .merge(item1, 1, Integer::sum);
                }
            }
        }
    }

    // Normalize by item frequencies to calculate relevance scores
    Map<String, Map<String, Double>> recommendations = new HashMap<>();
    for (Map.Entry<String, Map<String, Integer>> entry : rawCounts.entrySet()) {
        String item1 = entry.getKey();
        Map<String, Double> weightedScores = entry.getValue().entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue() / (double) (itemFrequency.get(item1) + itemFrequency.get(e.getKey()) - e.getValue())
            ));
        recommendations.put(item1, weightedScores);
    }
    return recommendations;
}



private static List<String> recommendProducts(Map<String, Map<String, Double>> recommendations, String product, int topN) {

    if (!recommendations.containsKey(product)) {
        return Collections.emptyList();
    }
    return recommendations.get(product).entrySet().stream()
            .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue())) // Sort by weighted score
            .limit(topN)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
}


public static void main(String[] args) throws IOException {
    String filepath = "C:\\Users\\Branb\\Kragger Product Recommendation\\Groceries_dataset.csv";
    
    // Load data and group items by customer
    Map<String, List<String>> customerPurchases = loadData(filepath);

    // Build the recommendation model based on weighted co-occurrence
    Map<String, Map<String, Double>> recommendations = buildRecommendationModel(customerPurchases);

    // Run the recommendation system
    System.out.println("Welcome to the product recommendation system!");
    Scanner scanner = new Scanner(System.in);

    while (true) {
        System.out.print("Enter a product name (or 'exit' to quit): ");
        String userProduct = scanner.nextLine().trim();
        if (userProduct.equalsIgnoreCase("exit")) {
            System.out.println("Thank you for using the recommendation system!");
            break;
        }

        // Get recommendations for the selected product
        List<String> suggestedProducts = recommendProducts(recommendations, userProduct, 5);
        if (!suggestedProducts.isEmpty()) {
            System.out.println("People who bought '" + userProduct + "' also bought:");
            for (int i = 0; i < suggestedProducts.size(); i++) {
                System.out.println((i + 1) + ". " + suggestedProducts.get(i));
            }
        } else {
            System.out.println("Sorry, no recommendations found for '" + userProduct + "'.");
        }
    }

    scanner.close();
}






}
