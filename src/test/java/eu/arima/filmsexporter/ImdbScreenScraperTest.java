package eu.arima.filmsexporter;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.temporal.TemporalField;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ImdbScreenScraperTest {

    @Test
    void scrapeFilmLink() {
        ImdbScreenScraper imdbScreenScraper = new ImdbScreenScraper();

        Film film = imdbScreenScraper.scrapeFilmLink("https://www.imdb.com/title/tt5433138/");

        assertEquals("Fast & Furious 9", film.getTitle());
    }

    @Test
    void scrapeMostPopularMovieLinks() {
        ImdbScreenScraper imdbScreenScraper = new ImdbScreenScraper();
        List<String> links = imdbScreenScraper.scrapeMovieListLinks("https://www.imdb.com/chart/moviemeter/");
        assertEquals(100, links.size());
    }

    @Test
    void parseDate() {
        ImdbScreenScraper imdbScreenScraper = new ImdbScreenScraper();

        LocalDate date1 = imdbScreenScraper.parseDate("7 February 2020");
        assertEquals(7, date1.getDayOfMonth());
        assertEquals(2, date1.getMonthValue());
        assertEquals(2020, date1.getYear());

        LocalDate date2 = imdbScreenScraper.parseDate("17 February 2020");
        assertEquals(17, date2.getDayOfMonth());
        assertEquals(2, date2.getMonthValue());
        assertEquals(2020, date2.getYear());
    }

}