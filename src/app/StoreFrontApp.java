package app;

import model.SalableProduct;
import service.AdministrationService;
import service.FileServiceException;
import service.StoreFront;

import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Console driver for the Store Front application.
 *
 * This class starts the user-facing store menu and also starts the
 * AdministrationService in the background so a local admin client can send
 * commands while the user is still interacting with the store.
 */
public class StoreFrontApp {

    /**
     * Inventory file used by the storefront and administration service.
     */
    private static final String INVENTORY_FILE_PATH = "inventory.json";

    /**
     * Loopback port used by the administration service.
     */
    private static final int ADMIN_PORT = 5050;

    /**
     * Program entry point.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        StoreFront storeFront = new StoreFront();

        try {
            storeFront.initializeStore(INVENTORY_FILE_PATH);
            System.out.println("Inventory loaded from JSON: " + INVENTORY_FILE_PATH);
        } catch (FileServiceException ex) {
            System.out.println("Failed to load inventory from JSON.");
            System.out.println("Reason: " + ex.getMessage());
            System.out.println("Falling back to default hardcoded inventory for this run.\n");

            storeFront.initializeStore();
            try {
                storeFront.saveInventoryToJson(INVENTORY_FILE_PATH);
                System.out.println("Default inventory was saved to JSON: " + INVENTORY_FILE_PATH);
            } catch (FileServiceException saveEx) {
                System.out.println("Warning: default inventory could not be saved to JSON.");
                System.out.println("Reason: " + saveEx.getMessage());
            }
        }

        AdministrationService administrationService =
                new AdministrationService(storeFront, INVENTORY_FILE_PATH, ADMIN_PORT);

        administrationService.start();
        System.out.println("Administration Service started on localhost:" + ADMIN_PORT);

        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        System.out.println("Welcome to the Fantasy Storefront!");

        while (running) {
            printMenu();
            System.out.print("Enter choice: ");

            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1":
                        printInventory(storeFront.getInventoryManager().listProducts());
                        break;
                    case "2":
                        handlePurchase(storeFront, scanner);
                        break;
                    case "3":
                        handleCancel(storeFront, scanner);
                        break;
                    case "4":
                        printCart(storeFront);
                        break;
                    case "5":
                        System.out.println("Cart Total: $" + storeFront.getCartTotal());
                        break;
                    case "6":
                        try {
                            storeFront.initializeStore(INVENTORY_FILE_PATH);
                            System.out.println("Store re-initialized from JSON.");
                        } catch (FileServiceException ex) {
                            System.out.println("Failed to re-load inventory from JSON.");
                            System.out.println("Reason: " + ex.getMessage());
                            System.out.println("Falling back to default hardcoded inventory.");

                            storeFront.initializeStore();
                            storeFront.saveInventoryToJson(INVENTORY_FILE_PATH);
                        }
                        break;
                    case "7":
                        handleSortInventory(storeFront, scanner);
                        break;
                    case "0":
                        running = false;
                        break;
                    default:
                        System.out.println("Invalid option.");
                        break;
                }
            } catch (RuntimeException ex) {
                System.out.println("Error: " + ex.getMessage());
            } catch (FileServiceException ex) {
                System.out.println("File error: " + ex.getMessage());
            }

            System.out.println();
        }

        administrationService.stop();
        System.out.println("Goodbye");
        scanner.close();
    }

    /**
     * Prints the main menu.
     */
    private static void printMenu() {
        System.out.println("Menu:");
        System.out.println("1) List Inventory");
        System.out.println("2) Purchase Product");
        System.out.println("3) Cancel Purchase");
        System.out.println("4) View Cart");
        System.out.println("5) View Cart Total");
        System.out.println("6) Re-Initialize Store");
        System.out.println("7) Sort Inventory");
        System.out.println("0) Exit");
    }

    /**
     * Prints the inventory list.
     *
     * @param products products to display
     */
    private static void printInventory(List<SalableProduct> products) {
        System.out.println("Inventory:");
        for (SalableProduct product : products) {
            System.out.println(product);
        }
    }

    /**
     * Handles the sort workflow.
     *
     * @param storeFront store front instance
     * @param scanner scanner for user input
     */
    private static void handleSortInventory(StoreFront storeFront, Scanner scanner) {
        System.out.print("Sort by name or price: ");
        String sortBy = scanner.nextLine().trim().toLowerCase();

        if (!sortBy.equals("name") && !sortBy.equals("price")) {
            throw new IllegalArgumentException("Sort field must be 'name' or 'price'.");
        }

        System.out.print("Sort order (asc or desc): ");
        String order = scanner.nextLine().trim().toLowerCase();

        if (!order.equals("asc") && !order.equals("desc")) {
            throw new IllegalArgumentException("Sort order must be 'asc' or 'desc'.");
        }

        boolean ascending = order.equals("asc");
        List<SalableProduct> sortedProducts = storeFront.getSortedProducts(sortBy, ascending);

        System.out.println("Sorted Inventory:");
        printInventory(sortedProducts);
    }

    /**
     * Handles product purchases from the console.
     *
     * @param storeFront store front instance
     * @param scanner scanner for user input
     * @throws FileServiceException when updated inventory cannot be saved
     */
    private static void handlePurchase(StoreFront storeFront, Scanner scanner) throws FileServiceException {
        System.out.print("Enter product name to purchase: ");
        String productName = scanner.nextLine();

        System.out.print("Enter quantity to purchase: ");
        int qty = Integer.parseInt(scanner.nextLine().trim());

        storeFront.purchaseProduct(productName, qty);
        System.out.println("Added to cart.");
    }

    /**
     * Handles purchase cancellation from the console.
     *
     * @param storeFront store front instance
     * @param scanner scanner for user input
     * @throws FileServiceException when updated inventory cannot be saved
     */
    private static void handleCancel(StoreFront storeFront, Scanner scanner) throws FileServiceException {
        System.out.print("Enter product name to cancel: ");
        String productName = scanner.nextLine();

        System.out.print("Enter quantity to cancel: ");
        int qty = Integer.parseInt(scanner.nextLine().trim());

        storeFront.cancelPurchase(productName, qty);
        System.out.println("Canceled from cart.");
    }

    /**
     * Prints the shopping cart.
     *
     * @param storeFront store front instance
     */
    private static void printCart(StoreFront storeFront) {
        Map<String, Integer> items = storeFront.getShoppingCart().getItems();

        if (items.isEmpty()) {
            System.out.println("Cart is empty.");
            return;
        }

        System.out.println("Cart:");
        for (Map.Entry<String, Integer> entry : items.entrySet()) {
            System.out.println(entry.getKey() + " x " + entry.getValue());
        }
    }
}
