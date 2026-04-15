package com.makemytour.config;

import com.makemytour.entity.Flight;
import com.makemytour.entity.Hotel;
import com.makemytour.entity.TravelStory;
import com.makemytour.entity.User;
import com.makemytour.enums.FlightStatus;
import com.makemytour.enums.StoryStatus;
import com.makemytour.enums.TravelCategory;
import com.makemytour.repository.FlightRepository;
import com.makemytour.repository.HotelRepository;
import com.makemytour.repository.TravelStoryRepository;
import com.makemytour.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Seeds the H2 in-memory database with sample flights, hotels, users and
 * approved travel stories so the application is immediately usable after startup.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final FlightRepository flightRepository;
    private final HotelRepository hotelRepository;
    private final UserRepository userRepository;
    private final TravelStoryRepository storyRepository;
    private final PasswordEncoder passwordEncoder;

    // Demo seed passwords are externalised so no literal credentials appear in source code.
    // Override via SEED_ADMIN_PASSWORD / SEED_ALICE_PASSWORD / SEED_BOB_PASSWORD env vars.
    @Value("${app.seed.admin.password}")
    private String seedAdminPassword;

    @Value("${app.seed.alice.password}")
    private String seedAlicePassword;

    @Value("${app.seed.bob.password}")
    private String seedBobPassword;

    @Override
    public void run(String... args) {
        seedUsers();
        seedFlights();
        seedHotels();
        seedStories();
        log.info("✅ Sample data seeding complete — flights: {}, hotels: {}, users: {}",
                flightRepository.count(), hotelRepository.count(), userRepository.count());
    }

    private void seedUsers() {
        if (userRepository.count() > 0) {
            log.info("Users already seeded — skipping");
            return;
        }
        try {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@makemytour.com")
                    .password(passwordEncoder.encode(seedAdminPassword))
                    .fullName("Admin User")
                    .phoneNumber("9999999999")
                    .roles(Set.of("ROLE_USER", "ROLE_ADMIN"))
                    .build();

            User user1 = User.builder()
                    .username("alice")
                    .email("alice@example.com")
                    .password(passwordEncoder.encode(seedAlicePassword))
                    .fullName("Alice Sharma")
                    .phoneNumber("9876543210")
                    .roles(Set.of("ROLE_USER"))
                    .build();

            User user2 = User.builder()
                    .username("bob")
                    .email("bob@example.com")
                    .password(passwordEncoder.encode(seedBobPassword))
                    .fullName("Bob Patel")
                    .phoneNumber("9876543211")
                    .roles(Set.of("ROLE_USER"))
                    .build();

            userRepository.saveAll(Set.of(admin, user1, user2));
            log.info("Seeded {} users", userRepository.count());
        } catch (Exception ex) {
            log.error("Failed to seed users — continuing with other seed data. Cause: {}", ex.getMessage());
        }
    }

    private void seedFlights() {
        if (flightRepository.count() > 0) {
            log.info("Flights already seeded — skipping");
            return;
        }
        try {
            LocalDateTime now = LocalDateTime.now();

            Flight f1 = new Flight();
            f1.setFlightNumber("AI101");
            f1.setName("Air India Delhi → Mumbai");
            f1.setDescription("Non-stop flight from Indira Gandhi International to Chhatrapati Shivaji");
            f1.setBasePrice(4500.00);
            f1.setCategory(TravelCategory.CITY_TOUR);
            f1.setOrigin("Delhi");
            f1.setDestination("Mumbai");
            f1.setDepartureTime(now.plusDays(1).withHour(6).withMinute(0));
            f1.setArrivalTime(now.plusDays(1).withHour(8).withMinute(10));
            f1.setStatus(FlightStatus.ON_TIME);
            f1.setSeatRows(30);
            f1.setSeatCols(6);
            // Row 1 seats are premium (extra legroom at front)
            f1.setPremiumSeats(Set.of("1A", "1B", "1C", "1D", "1E", "1F", "2A", "2F"));

            Flight f2 = new Flight();
            f2.setFlightNumber("6E201");
            f2.setName("IndiGo Mumbai → Goa");
            f2.setDescription("Quick hop from Mumbai to the beach paradise of Goa");
            f2.setBasePrice(3200.00);
            f2.setCategory(TravelCategory.BEACH);
            f2.setOrigin("Mumbai");
            f2.setDestination("Goa");
            f2.setDepartureTime(now.plusDays(2).withHour(9).withMinute(30));
            f2.setArrivalTime(now.plusDays(2).withHour(10).withMinute(45));
            f2.setStatus(FlightStatus.ON_TIME);
            f2.setSeatRows(32);
            f2.setSeatCols(6);
            f2.setPremiumSeats(Set.of("1A", "1B", "1C", "1D", "1E", "1F"));

            Flight f3 = new Flight();
            f3.setFlightNumber("SG301");
            f3.setName("SpiceJet Delhi → Shimla");
            f3.setDescription("Scenic flight to the queen of hill stations");
            f3.setBasePrice(5800.00);
            f3.setCategory(TravelCategory.HILL_STATION);
            f3.setOrigin("Delhi");
            f3.setDestination("Shimla");
            f3.setDepartureTime(now.plusDays(3).withHour(7).withMinute(0));
            f3.setArrivalTime(now.plusDays(3).withHour(8).withMinute(0));
            f3.setStatus(FlightStatus.ON_TIME);
            f3.setSeatRows(20);
            f3.setSeatCols(4);
            f3.setPremiumSeats(Set.of("1A", "1B", "1C", "1D"));

            Flight f4 = new Flight();
            f4.setFlightNumber("UK401");
            f4.setName("Vistara Mumbai → Jaipur");
            f4.setDescription("Fly to the Pink City – a heritage lover's dream");
            f4.setBasePrice(6200.00);
            f4.setCategory(TravelCategory.HERITAGE);
            f4.setOrigin("Mumbai");
            f4.setDestination("Jaipur");
            f4.setDepartureTime(now.plusDays(4).withHour(11).withMinute(0));
            f4.setArrivalTime(now.plusDays(4).withHour(12).withMinute(30));
            f4.setStatus(FlightStatus.ON_TIME);
            f4.setSeatRows(28);
            f4.setSeatCols(6);
            f4.setPremiumSeats(Set.of("1A", "1B", "1C", "1D", "1E", "1F", "2A", "2F"));

            Flight f5 = new Flight();
            f5.setFlightNumber("G8501");
            f5.setName("GoAir Bangalore → Cochin");
            f5.setDescription("Fly to Kerala – God's Own Country for the perfect honeymoon");
            f5.setBasePrice(4800.00);
            f5.setCategory(TravelCategory.HONEYMOON);
            f5.setOrigin("Bangalore");
            f5.setDestination("Cochin");
            f5.setDepartureTime(now.plusDays(5).withHour(14).withMinute(0));
            f5.setArrivalTime(now.plusDays(5).withHour(15).withMinute(15));
            f5.setStatus(FlightStatus.ON_TIME);
            f5.setSeatRows(30);
            f5.setSeatCols(6);
            f5.setPremiumSeats(Set.of("1A", "1B", "1C", "1D", "1E", "1F"));

            flightRepository.saveAll(java.util.List.of(f1, f2, f3, f4, f5));
            log.info("Seeded {} flights", flightRepository.count());
        } catch (Exception ex) {
            log.error("Failed to seed flights — continuing with other seed data. Cause: {}", ex.getMessage());
        }
    }

    private void seedHotels() {
        if (hotelRepository.count() > 0) {
            log.info("Hotels already seeded — skipping");
            return;
        }
        try {
            Hotel h1 = new Hotel();
            h1.setName("The Taj Mahal Palace");
            h1.setDescription("Iconic luxury hotel overlooking the Gateway of India");
            h1.setBasePrice(15000.00);
            h1.setCategory(TravelCategory.CITY_TOUR);
            h1.setCity("Mumbai");
            h1.setAddress("Apollo Bunder, Colaba, Mumbai 400001");
            h1.setStarRating(5);
            h1.setTotalRooms(60);
            h1.setRoomRows(6);
            h1.setRoomCols(10);
            h1.setAmenities("WiFi,Pool,Spa,Gym,Restaurant,Bar,Concierge");

            Hotel h2 = new Hotel();
            h2.setName("Goa Marriott Resort");
            h2.setDescription("Beachfront resort with stunning sea views in Panaji");
            h2.setBasePrice(9500.00);
            h2.setCategory(TravelCategory.BEACH);
            h2.setCity("Goa");
            h2.setAddress("Miramar Beach, Panaji, Goa 403001");
            h2.setStarRating(5);
            h2.setTotalRooms(48);
            h2.setRoomRows(4);
            h2.setRoomCols(12);
            h2.setAmenities("WiFi,BeachAccess,Pool,WaterSports,Restaurant");

            Hotel h3 = new Hotel();
            h3.setName("Wildflower Hall Shimla");
            h3.setDescription("Heritage mountain retreat surrounded by cedar forests");
            h3.setBasePrice(18000.00);
            h3.setCategory(TravelCategory.HILL_STATION);
            h3.setCity("Shimla");
            h3.setAddress("Chharabra, Shimla, Himachal Pradesh 171012");
            h3.setStarRating(5);
            h3.setTotalRooms(30);
            h3.setRoomRows(3);
            h3.setRoomCols(10);
            h3.setAmenities("WiFi,Spa,IndoorPool,Trekking,Restaurant");

            Hotel h4 = new Hotel();
            h4.setName("Hotel Clarks Amer");
            h4.setDescription("Elegant hotel in the heart of Jaipur near Amber Fort");
            h4.setBasePrice(7500.00);
            h4.setCategory(TravelCategory.HERITAGE);
            h4.setCity("Jaipur");
            h4.setAddress("Jawaharlal Nehru Marg, Jaipur 302018");
            h4.setStarRating(4);
            h4.setTotalRooms(40);
            h4.setRoomRows(4);
            h4.setRoomCols(10);
            h4.setAmenities("WiFi,Pool,Gym,Restaurant,HeritageTour");

            Hotel h5 = new Hotel();
            h5.setName("Kumarakom Lake Resort");
            h5.setDescription("Award-winning backwater resort in Kerala's lake district");
            h5.setBasePrice(22000.00);
            h5.setCategory(TravelCategory.HONEYMOON);
            h5.setCity("Kottayam");
            h5.setAddress("Kumarakom, Kottayam, Kerala 686563");
            h5.setStarRating(5);
            h5.setTotalRooms(24);
            h5.setRoomRows(3);
            h5.setRoomCols(8);
            h5.setAmenities("WiFi,PrivatePool,Houseboat,Ayurveda,Restaurant");

            hotelRepository.saveAll(java.util.List.of(h1, h2, h3, h4, h5));
            log.info("Seeded {} hotels", hotelRepository.count());
        } catch (Exception ex) {
            log.error("Failed to seed hotels — continuing with other seed data. Cause: {}", ex.getMessage());
        }
    }

    private void seedStories() {
        if (storyRepository.count() > 0) {
            log.info("Travel stories already seeded — skipping");
            return;
        }
        try {
            // alice and bob are always seeded by seedUsers() before this runs
            java.util.Optional<User> aliceOpt = userRepository.findByUsername("alice");
            java.util.Optional<User> bobOpt   = userRepository.findByUsername("bob");

            if (aliceOpt.isEmpty()) {
                log.warn("Skipping story seed – alice user not found (user seeding may have failed)");
                return;
            }

            User alice = aliceOpt.get();
            User bob   = bobOpt.orElse(alice);

            TravelStory s1 = TravelStory.builder()
                    .author(alice)
                    .title("A Week in Goa – Sun, Sand & Seafood")
                    .content("<p>Goa stole my heart the moment I stepped off the plane. The warm breeze, the sound of waves at <strong>Baga Beach</strong>, and the aroma of freshly grilled king prawns at the shacks – every sense comes alive here.</p><p>We rented scooters and explored hidden beaches like <em>Butterfly Beach</em> and <em>Cola Beach</em> that most tourists never find. The nightlife at Tito's Lane was electric, but what I'll remember most is watching the sun set behind the Portuguese-era churches of Old Goa.</p><p>Pro tip: Visit in November for perfect weather and fewer crowds!</p>")
                    .destination("Goa")
                    .coverImageUrl("https://images.unsplash.com/photo-1512343879784-a960bf40e7f2?w=800")
                    .status(StoryStatus.APPROVED)
                    .build();

            TravelStory s2 = TravelStory.builder()
                    .author(bob)
                    .title("Shimla Diaries – My First Hill Station Experience")
                    .content("<p>The toy train from Kalka to Shimla is an experience that no car ride can replicate. Winding through 102 tunnels and across 864 bridges, every turn reveals a more breathtaking view than the last.</p><p>Shimla's Mall Road is charming at dusk – sip hot chai from a roadside stall and watch locals promenade in warm woolens. We took a day trip to <strong>Kufri</strong> and tried yak rides in the snow!</p><p>The Wildflower Hall hotel was an absolute dream – cedar forests, a heated pool, and stargazing from the terrace. Worth every rupee for a special occasion.</p>")
                    .destination("Shimla")
                    .coverImageUrl("https://images.unsplash.com/photo-1626621341517-bbf3d9990a23?w=800")
                    .status(StoryStatus.APPROVED)
                    .build();

            TravelStory s3 = TravelStory.builder()
                    .author(alice)
                    .title("Jaipur in 3 Days – The Pink City Lives Up to Its Name")
                    .content("<p>Three days is barely enough to scratch the surface of Jaipur, but we made the most of every hour. The <strong>Amber Fort</strong> at sunrise, when the crowds are thin and the golden light bounces off the sandstone, is one of those images that stay with you forever.</p><p>The bazaars of Johari Bazaar are an explosion of colour – block-printed fabrics, blue pottery, and the most intricate silver jewellery you'll ever see. Don't miss the <em>dal baati churma</em> at a local dhaba; it is the soul of Rajasthani food.</p><p>A heritage walk through the walled city at night, when it's lit up like a lantern, was the perfect way to end our trip.</p>")
                    .destination("Jaipur")
                    .coverImageUrl("https://images.unsplash.com/photo-1477587458883-47145ed31d30?w=800")
                    .status(StoryStatus.APPROVED)
                    .build();

            storyRepository.saveAll(List.of(s1, s2, s3));
            log.info("Seeded {} approved travel stories", storyRepository.count());
        } catch (Exception ex) {
            log.error("Failed to seed travel stories — continuing. Cause: {}", ex.getMessage());
        }
    }
}
