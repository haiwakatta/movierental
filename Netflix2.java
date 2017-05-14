import java.sql.*;
import java.util.*;

public class Netflix2 {

    public static void main(String[] args) throws Exception {
        int currentUser = 0;
        String currentName;
        String movieId;
        String movieStatus;
        Class.forName("com.mysql.jdbc.Driver");
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/customer", "root", "Password123");
        Connection connIMDB = DriverManager.getConnection("jdbc:mysql://localhost/IMDB", "root", "Password123");
        conn.setAutoCommit(false);
        connIMDB.setAutoCommit(false);
        Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        Statement stmtIMDB = connIMDB.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        Statement stmtIMDB1 = connIMDB.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        Statement stmtIMDB2 = connIMDB.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

        String loginAttempt = "SELECT ID FROM customer WHERE login=\"" + args[0] + "\" AND password=\"" + args[1] + "\"";
        ResultSet login = stmt.executeQuery(loginAttempt);

        //check if login is correct
        if (!login.next()) {
            System.out.println("Your Username or Password is incorrect");
        } else
            currentUser=login.getInt(1);

        //Get their firstName
        String getName = "SELECT FIRST_NAME FROM CUSTOMER WHERE ID=" + currentUser;
        ResultSet queryName = stmt.executeQuery(getName);
        queryName.next();
        currentName = queryName.getString(1);

        System.out.println("Welcome to Netflix 2.0 " + currentName);

        System.out.println("Type Rent or Return followed by a movie ID or Search followed by a movie title");

        Scanner sc = new Scanner(System.in);
        while (sc.hasNext()) {
            String search = sc.next();
            switch (search) {
                //Rent a movie
                case "Rent":
                    movieId = sc.next();

                    String moviesRented = "SELECT  COUNT(*) FROM rental WHERE customer_id=" + currentUser + " AND status = \"open\"";
                    ResultSet rentedMovies = stmt.executeQuery(moviesRented);
                    rentedMovies.next();
                    if(rentedMovies.getInt(1) >= 3){
                        System.out.println("You already have three movies rented, please return some movies before renting new ones");
                        break;
                    }

                    String rentMovie = "SELECT  ID, STATUS FROM rental WHERE movie_id =\"" + movieId + "\" ORDER by ID DESC LIMIT 1";
                    ResultSet rentMovieQuery = stmt.executeQuery(rentMovie);

                    if (!rentMovieQuery.next()){
                        movieStatus = "closed";
                    }
                    else
                    {movieStatus= rentMovieQuery.getString(2);}

                    if (movieStatus.equals("closed")) {
                        //create new row into rental
                        PreparedStatement rentMovieStatement = conn.prepareStatement("INSERT INTO rental (movie_id, status, customer_id) VALUES (?,?,?)");
                        rentMovieStatement.setInt(1, Integer.parseInt(movieId));
                        rentMovieStatement.setString(2, "open");
                        rentMovieStatement.setInt(3, currentUser);
                        rentMovieStatement.executeUpdate(); // Execute prepared statement with current parameters
                        conn.commit(); // Commit all pending updates
                        System.out.println("You're movie has now been rented");
                        } else
                    {System.out.println("The movie you're trying to rent is not available, please start over");}

                    break;

                    //return a movie
                case "Return":
                    movieId = sc.next();

                    String returnMovie = "SELECT  ID, STATUS FROM rental WHERE movie_id =\"" + movieId + "\" ORDER by ID DESC LIMIT 1";
                    ResultSet returnMovieQuery = stmt.executeQuery(returnMovie);
                    int returnId;
                    returnMovieQuery.next();
                    movieStatus = returnMovieQuery.getString(2);
                    if (movieStatus.equals("closed")){
                        System.out.println("The movie you entered has not been checked out. You may have entered the wrong movie ID Please try again");}
                    else {
                        returnId = returnMovieQuery.getInt(1);
                        PreparedStatement returnMovieStatement = conn.prepareStatement("UPDATE rental set status = \"closed\" where id =" + returnId);
                        returnMovieStatement.executeUpdate();
                        conn.commit(); // Commit all pending updates
                        System.out.println("You're movie has now been returned");
                    }
                    break;

                case "Search":
                    String movieTitle = sc.nextLine().substring(1);
                    String searchMovieTitle = "SELECT  ID, TITLE FROM movie WHERE title LIKE \"%"+movieTitle+ "%\" limit 5";
                    ResultSet searchMovieTitleResult = stmtIMDB.executeQuery(searchMovieTitle);
                    ArrayList<String> directors = new ArrayList<String>();
                    ArrayList<String> actors = new ArrayList<String>();


                    while (searchMovieTitleResult.next()){
                        int searchMovieId = searchMovieTitleResult.getInt(1);
                        String searchMovieString = searchMovieTitleResult.getString(2);

                        //loop through involved table collecting directors
                        String searchDirectorId = "SELECT  personID FROM involved WHERE movieId = \""+searchMovieId+ "\" AND role = \"director\" ";
                        ResultSet searchDirectorIdResult = stmtIMDB1.executeQuery(searchDirectorId);


                        while (searchDirectorIdResult.next()){
                            String searchDirectorName = "SELECT name FROM person WHERE Id = \""+ searchDirectorIdResult.getString(1)+ "\"";
                            ResultSet searchDirectorNameResult = stmtIMDB2.executeQuery(searchDirectorName);

                            while (searchDirectorNameResult.next()){
                                directors.add(searchDirectorNameResult.getString(1));
                            }
                        }

                        //loop through involved table collecting actors
                        String searchActorId = "SELECT  personID FROM involved WHERE movieId = \""+searchMovieId+ "\" AND role = \"actor\" ";
                        ResultSet searchActorIdResult = stmtIMDB1.executeQuery(searchActorId);

                        while (searchActorIdResult.next()){
                            String searchActorName = "SELECT name FROM person WHERE Id = \""+ searchActorIdResult.getString(1)+ "\"";
                            ResultSet searchActorNameResult = stmtIMDB2.executeQuery(searchActorName);

                            while (searchActorNameResult.next()){
                                actors.add(searchActorNameResult.getString(1));
                            }
                        }
                        //print movie titles and people involved
                        System.out.println("ID: "+ searchMovieId + " Title: " + searchMovieString);
                        System.out.println("Directed by:");
                        for(int i =0; i<3;i++){
                            System.out.println(directors.get(i));
                        }
                        System.out.println("Starring:");
                        for(int i =0; i<6;i++){
                            System.out.println(actors.get(i));
                        }
                        String searchAvailable = "SELECT status FROM rental WHERE movie_id = \""+ searchMovieId+ "\" limit 1";
                        ResultSet searchMovieAvailability = stmt.executeQuery(searchAvailable);
                        Boolean available = true;
                        if (searchMovieAvailability.getString(1).equals("open")){
                            available = false;
                        }

                        //print availability
                        if(available){
                            System.out.println("This movie is currently available for renting");
                        }
                        else {
                            System.out.println("This movie is not currently available");
                        }
                    }


            }
        }
        conn.close();
    }
}