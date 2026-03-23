

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.sql.Connection;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Connection con = DBconnection.getConnection();
        if (con != null) {
            System.out.println("Connected to library_db!");
        } else {
            System.out.println("Failed to connect to library_db!");
            return;
        }

        BookService bookService = new BookService();
        AdminService adminService = new AdminService();
        StudentService studentService = new StudentService();

        int port = 8080;
        boolean started = false;
        for (; port < 8100; port++) {
            try {
                new LibraryApiServer(adminService, bookService, studentService).start(port);
                started = true;
                break;
            } catch (java.io.IOException e) {
                if (e.getMessage() != null && e.getMessage().contains("Address already in use")) {
                    System.err.println("Port " + port + " is already in use, trying next port...");
                    continue;
                }
                System.err.println("Failed to start API server: " + e.getMessage());
                return;
            }
        }

        if (!started) {
            System.err.println("Failed to bind API server to any port between 8080 and 8099");
            return;
        }

        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI("http://localhost:" + port));
            }
        } catch (Exception e) {
            System.err.println("Failed to launch browser: " + e.getMessage());
        }

        System.out.println("Backend is running. Open your browser at http://localhost:8080.");
        System.out.println("Press CTRL+C to stop.");

        // Keep the application alive while the HTTP server is running.
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void adminMenu(Scanner scanner, AdminService adminService, BookService bookService) {
        while (true) {
            System.out.println("\n=== Admin Menu ===");
            System.out.println("1. Add Book");
            System.out.println("2. Issue Book");
            System.out.println("3. View Records (all issued)");
            System.out.println("0. Back");
            System.out.print("Choice: ");
            int option = scanner.nextInt();
            scanner.nextLine();

            if (option == 0) {
                break;
            }
            switch (option) {
                case 1 -> {
                    System.out.print("Title: ");
                    String title = scanner.nextLine();
                    System.out.print("Author: ");
                    String author = scanner.nextLine();
                    System.out.print("Category: ");
                    String category = scanner.nextLine();
                    System.out.print("ISBN: ");
                    String isbn = scanner.nextLine();
                    System.out.print("Available (true/false): ");
                    boolean available = scanner.nextBoolean();
                    scanner.nextLine();

                    Book book = new Book(0, title, author, category, isbn, available);
                    if (adminService.addBook(book)) {
                        System.out.println("Book added successfully.");
                    } else {
                        System.out.println("Failed to add book.");
                    }
                }
                case 2 -> {
                    System.out.print("Student ID: ");
                    int studentId = scanner.nextInt();
                    System.out.print("Book ID: ");
                    int bookId = scanner.nextInt();
                    scanner.nextLine();
                    if (bookService.issueBook(studentId, bookId)) {
                        System.out.println("Book issued successfully.");
                    } else {
                        System.out.println("Failed to issue book.");
                    }
                }
                case 3 -> {
                    List<IssueRecord> records = adminService.viewAllIssuedBooks();
                    if (records.isEmpty()) {
                        System.out.println("No issued records found.");
                    } else {
                        records.forEach(System.out::println);
                    }
                }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private static void studentMenu(Scanner scanner, StudentService studentService, BookService bookService) {
        System.out.print("Student ID: ");
        int studentId = scanner.nextInt();
        scanner.nextLine();

        while (true) {
            System.out.println("\n=== Student Menu ===");
            System.out.println("1. View My Books");
            System.out.println("2. Return Book");
            System.out.println("0. Back");
            System.out.print("Choice: ");
            int option = scanner.nextInt();
            scanner.nextLine();

            if (option == 0) { break; }
            switch (option) {
                case 1 -> {
                    List<IssueRecord> issued = studentService.viewIssuedBooks(studentId);
                    if (issued.isEmpty()) {
                        System.out.println("No books issued currently.");
                    } else {
                        issued.forEach(System.out::println);
                    }
                }
                case 2 -> {
                    System.out.print("Book ID to return: ");
                    int bookId = scanner.nextInt();
                    scanner.nextLine();
                    if (bookService.returnBook(studentId, bookId)) {
                        System.out.println("Book returned successfully.");
                    } else {
                        System.out.println("Failed to return book.");
                    }
                }
                default -> System.out.println("Invalid option.");
            }
        }
    }
}