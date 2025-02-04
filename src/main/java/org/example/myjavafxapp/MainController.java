package org.example.myjavafxapp;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
import org.example.myjavafxapp.model.Book;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainController {

    @FXML
    private TableView<Book> bookTable;
    @FXML
    private TableColumn<Book, Long> idColumn;
    @FXML
    private TableColumn<Book, String> titleColumn;
    @FXML
    private TableColumn<Book, String> authorColumn;
    @FXML
    private TableColumn<Book, String> isbnColumn;
    @FXML
    private TableColumn<Book, LocalDate> publishedDateColumn;
    @FXML
    private TextField searchField;
    @FXML
    private Label pageLabel;
    @FXML
    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 2;

    @FXML
    private Pagination pagination;
    // Observable list of books
    private ObservableList<Book> bookList = FXCollections.observableArrayList();

    @FXML
    private TextField titleField;
    @FXML
    private TextField authorField;
    @FXML
    private TextField isbnField;
    @FXML
    private TextField publishedDate;


    private final String BASE_URL = "http://localhost:8080/api/v1/books";
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
        isbnColumn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        publishedDateColumn.setCellValueFactory(new PropertyValueFactory<>("publishedDate"));
        //====>>>>>>>>> Load data from backend
        handleRefresh();
       //update the pagination label
        updatePageLabel();
        // ==>>this set up a listener for the search field to filter books dynamically
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterBooks(newValue);
        });
        // this set the pagination's page factory to dynamically generate pages
        pagination.setPageFactory(this::createPage);
    }

    private void filterBooks(String searchText) {
        if (bookList == null || bookList.isEmpty()) {
            return;
        }
        // create a filtered list based on search input
        ObservableList<Book> filteredList = FXCollections.observableArrayList();
        for (Book book : bookList) {
            if (book.getTitle().toLowerCase().contains(searchText.toLowerCase()) ||
                    book.getAuthor().toLowerCase().contains(searchText.toLowerCase()) ||
                    book.getIsbn().toLowerCase().contains(searchText.toLowerCase())) {
                filteredList.add(book);
            }
        }

        bookTable.setItems(filteredList);
    }

    @FXML
    private void handleAdd() {
        //==> create a new book object from the form fields
        Book book = new Book();
        book.setTitle(titleField.getText());
        book.setAuthor(authorField.getText());
        book.setIsbn(isbnField.getText());
        book.setPublishedDate(publishedDate.getText());

        String jsonBody = new JSONObject()
                .put("title", book.getTitle())
                .put("author", book.getAuthor())
                .put("isbn", book.getIsbn())
                .put("publishedDate", book.getPublishedDate().toString())
                .toString();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        try {
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            handleRefresh();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleUpdate() {
        // get the selecte d book,...
        Book selectedBook = bookTable.getSelectionModel().getSelectedItem();
        if (selectedBook != null) {
           // update book details based on form input
            selectedBook.setTitle(titleField.getText());
            selectedBook.setAuthor(authorField.getText());
            selectedBook.setIsbn(isbnField.getText());
            selectedBook.setPublishedDate(publishedDate.getText());

            // convert updated book details to JSON
            String jsonBody = new JSONObject()
                    .put("title", selectedBook.getTitle())
                    .put("author", selectedBook.getAuthor())
                    .put("isbn", selectedBook.getIsbn())
                    .put("publishedDate", selectedBook.getPublishedDate().toString())
                    .toString();

            // ===>>create an HTTP PUT request to update the book
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/" + selectedBook.getId()))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            try {
                // sends the request and refresh the table after updating and the refresh the table
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                handleRefresh(); // Refresh the table after updating
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleDelete() {
        Book selectedBook = bookTable.getSelectionModel().getSelectedItem();
        if (selectedBook != null) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/" + selectedBook.getId()))
                    .DELETE()
                    .build();

            try {
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                handleRefresh();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleRefresh() {
        // create an HTTP GET request to fetch all books from the backend
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .GET()
                .build();

        System.out.println("Fetching books from backend...");

        try {
            // send the request and process the response
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();
            System.out.println("Response Body: " + responseBody);

            List<Book> bookArrayList = new ArrayList<>();

            // andle different Json formats returned by the backend
            if (responseBody.startsWith("{")) {
                System.out.println("Expected JSONArray but received JSONObject. Handling accordingly.");
                JSONObject jsonObject = new JSONObject(responseBody);

                // extract book list if the JSON object contains a "books" key
                if (jsonObject.has("books")) {
                    JSONArray booksJsonArray = jsonObject.getJSONArray("books");
                    bookArrayList = processBooksJsonArray(booksJsonArray);
                } else {
                    System.out.println("Error: JSON Object does not contain 'books' key.");
                }
            } else if (responseBody.startsWith("[")) {
                // directly parse the JSON array if it's in the correct format
                JSONArray booksJsonArray = new JSONArray(responseBody);
                bookArrayList = processBooksJsonArray(booksJsonArray);
            } else {
                System.out.println("Unexpected JSON format: " + responseBody);
            }

            // convert the parsed book list to an observable list
            bookList = FXCollections.observableArrayList(bookArrayList);

            // f there are books, update pagination controls
            if (bookList != null && !bookList.isEmpty()) {
                updatePagination();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void updatePagination() {
        if (bookList != null && !bookList.isEmpty()) {
            currentPage = 0;
            System.out.println("====>>>>> 2222222222222");
            updateTableView(getBooksForPage(currentPage));
        } else {
            pagination.setPageCount(0);
        }
    }

    private List<Book> processBooksJsonArray(JSONArray booksJsonArray) {
        List<Book> books = new ArrayList<>();
        for (int i = 0; i < booksJsonArray.length(); i++) {
            JSONObject bookJson = booksJsonArray.getJSONObject(i);
            Book book = new Book();
            book.setId(bookJson.getLong("id"));
            book.setTitle(bookJson.getString("title"));
            book.setAuthor(bookJson.getString("author"));
            book.setIsbn(bookJson.getString("isbn"));
            book.setPublishedDate(bookJson.getString("publishedDate"));

            books.add(book);
        }
        return books;
    }


    private Node createPage(int pageIndex) {
        currentPage = pageIndex;
        updatePageLabel();
        int startIndex = pageIndex * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, bookList.size());

        if (startIndex >= bookList.size()) {
            return new StackPane(new Label("No more pages available"));
        }
        ObservableList<Book> pageData = FXCollections.observableArrayList(bookList.subList(startIndex, endIndex));

        bookTable.setItems(pageData);
        bookTable.setMinHeight(400);
        return new StackPane(bookTable);
    }


    public void handlePreviousPage() {
        if (currentPage > 0) {
            currentPage--;
            updateTableView(getBooksForPage(currentPage));
            updatePageLabel();
        }
    }


    public void handleNextPage() {
        if (currentPage < getTotalPages() - 1) {
            currentPage++;
            updateTableView(getBooksForPage(currentPage));
            updatePageLabel();
        }
    }


    private void updatePageLabel() {
        int totalPages = getTotalPages();
        pageLabel.setText("Page " + (currentPage + 1) + " of " + totalPages);
    }

    private int getTotalPages() {
        return (int) Math.ceil((double) bookList.size() / ITEMS_PER_PAGE);
    }


    private List<Book> getBooksForPage(int pageIndex) {

        if (bookList == null || bookList.isEmpty()) {
            System.out.println("Book list is empty or null.");
            return Collections.emptyList();
        }

        int start = pageIndex * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, bookList.size());

        if (start >= bookList.size()) {
            System.out.println("Start index is out of bounds.");
            return Collections.emptyList();
        }
        return bookList.subList(start, end);
    }


    private void updateTableView(List<Book> books) {
        if (books == null || books.isEmpty()) {
            System.out.println("No books to display.");
            bookTable.setItems(FXCollections.observableArrayList());
            bookTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        } else {
            System.out.println("Updating table view with books for the next page.");
            bookTable.setItems(FXCollections.observableArrayList(books));
        }
    }
}
