package fm.bootifulpodcast.integration;

import fm.bootifulpodcast.integration.database.User;
import fm.bootifulpodcast.integration.database.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// todo go back in history and restore the SecurityConfiguration that used to be here
// todo also go back into the pom.xml and restore the jwt-spring-boot-starter

@Configuration
@Slf4j
class CorsConfig {

	@Slf4j
	@Configuration
	public static class MyWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			log.info("launching " + MyWebSecurityConfigurerAdapter.class.getName());
			http //
					.authorizeRequests(ae -> ae //
							.mvcMatchers("/podcasts/search").authenticated() //
							.mvcMatchers("/podcasts/index").authenticated() //
							.mvcMatchers(HttpMethod.POST, "/podcasts/**").authenticated() //
							.mvcMatchers("/podcasts/*/profile-photo").permitAll() //
							.mvcMatchers("/podcasts/*/produced-audio").permitAll() //
							.mvcMatchers("/podcasts").authenticated() //
							.mvcMatchers("/actuator/health").permitAll() //
							.mvcMatchers("/actuator/health/**").permitAll() //
							.mvcMatchers("/site/podcasts").permitAll() //
							.mvcMatchers("/admin/**").authenticated() //
							.requestMatchers(EndpointRequest.toAnyEndpoint()).authenticated()//
							.anyRequest().permitAll() //
					) //
					.cors(Customizer.withDefaults())//
					.oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt)//
					.csrf(AbstractHttpConfigurer::disable);
		}

		/*
		 * @Bean CorsConfigurationSource corsConfigurationSource() { var methods = Stream
		 * .of(HttpMethod.POST, HttpMethod.OPTIONS, HttpMethod.DELETE, HttpMethod.PUT,
		 * HttpMethod.GET)// .map(Enum::name)// .collect(Collectors.toList());
		 *
		 * var configuration = new CorsConfiguration();
		 * configuration.setAllowCredentials(true);
		 * configuration.setAllowedHeaders(List.of("*"));
		 * configuration.setAllowedOrigins(List.of("*"));
		 * configuration.setAllowedMethods(methods);
		 *
		 * var source = new UrlBasedCorsConfigurationSource();
		 * source.registerCorsConfiguration("/**", configuration);
		 *
		 * return source; }
		 */

	}

	@Bean
	UserDetailsService jdbcUserDetailsService(UserRepository repository) {
		return new JdbcUserDetailsService(repository);
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				log.info("enabling global CORS supports");
				var methods = Stream
						.of(HttpMethod.POST, HttpMethod.OPTIONS, HttpMethod.DELETE, HttpMethod.PUT, HttpMethod.GET)//
						.map(Enum::name).toList();

				registry.addMapping("/**")//
						.allowedMethods(methods.toArray(new String[0]))//
						.allowedOriginPatterns("*");
			}
		};
	}

}

@Slf4j
@RequiredArgsConstructor
class JdbcUserDetailsService implements UserDetailsService {

	@RequiredArgsConstructor
	private static class JpaUserDetails implements UserDetails {

		private final User user;

		@Override
		public Collection<? extends GrantedAuthority> getAuthorities() {
			return Collections.singleton(new SimpleGrantedAuthority("USER"));
		}

		@Override
		public String getPassword() {
			return this.user.getPassword();
		}

		@Override
		public String getUsername() {
			return this.user.getUsername();
		}

		@Override
		public boolean isAccountNonExpired() {
			return true;
		}

		@Override
		public boolean isAccountNonLocked() {
			return true;
		}

		@Override
		public boolean isCredentialsNonExpired() {
			return true;
		}

		@Override
		public boolean isEnabled() {
			return true;
		}

	}

	private final UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

		var byUsername = this.userRepository.findByUsernameIgnoreCase((username + "").toLowerCase()).stream()
				.map(JpaUserDetails::new).toList();

		if (byUsername.size() != 1) {
			throw new UsernameNotFoundException(
					"couldn't find one and only one instance of the user '" + username + "' ");
		}

		return byUsername.get(0);
	}

}