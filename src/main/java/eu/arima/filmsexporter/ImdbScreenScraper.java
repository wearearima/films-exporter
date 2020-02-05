package eu.arima.filmsexporter;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.util.stream.Collectors.*;

public class ImdbScreenScraper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImdbScreenScraper.class);

    private static final String IMDB_ROOT_ADDRESS = "https://www.imdb.com";

    List<String> scrapeMovieListLinks(String movieListLink) {
        Document doc = getWebDocument(movieListLink);
        Elements linkElements = doc.select(".titleColumn a");

        return linkElements.stream()
                .map(e -> IMDB_ROOT_ADDRESS + e.attr("href"))
                .collect(toList());
    }

    Film scrapeFilmLink(String link) {
        try {
            Document doc = getWebDocument(link);

            Film film = new Film();

            Element titleWrapperElement = doc.select(".title_wrapper").first();

            String titleWithYear = titleWrapperElement.getElementsByTag("h1").get(0).text();
            String title = removeBrackets(titleWithYear);
            film.setTitle(title);

            String description = doc.select(".summary_text").first().text().trim();
            film.setDescription(description);

            Element subtextElement = titleWrapperElement.getElementsByClass("subtext").first();
            Elements timeElements = subtextElement.getElementsByTag("time");

            if (timeElements == null || timeElements.size() == 0) {
                LOGGER.info("Film {} without time", link);
            } else {
                String time = timeElements.first().text();
                film.setTime(time);
            }

            Elements links = subtextElement.getElementsByTag("a");

            String dateWithCountry = links.stream()
                    .filter(e -> e.hasAttr("title"))
                    .findFirst()
                    .get().text();

            String date = removeBrackets(dateWithCountry);
            film.setReleaseDate(parseDate(date));

            List<String> genreList = links.stream()
                    .filter(e -> !e.hasAttr("title"))
                    .map(o -> o.text())
                    .collect(toList());
            film.setGenreList(genreList);

            Element posterElement = doc.select(".poster a img").first();
            String posterImageLink = posterElement.attr("src");

            BufferedImage image = ImageIO.read(new URL(posterImageLink));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", outputStream);
            byte[] imageByteArray = outputStream.toByteArray();
            film.setImage(imageByteArray);

            return film;
        } catch (Exception e) {
            LOGGER.info("Error parsing film {}", link);
            throw new ScreenScrapingException(e);
        }
    }

    LocalDate parseDate(String text) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy");
        return LocalDate.parse(text, dateTimeFormatter);
    }

    private String removeBrackets(String text) {
        return text.substring(0, text.indexOf(" ("));
    }

    private Document getWebDocument(String link) {
        try {
            return Jsoup.connect(link).get();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

}
