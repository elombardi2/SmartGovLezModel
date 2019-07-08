package com.smartgov.lez.controller;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import com.smartgov.lez.controller.SmartGovController;

import smartgov.SmartGov;
import smartgov.core.events.EventHandler;
import smartgov.core.simulation.events.SimulationStep;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class SmartGovControllerTest {
	
	@Autowired
    private MockMvc mvc;
	
	@Test
	public void build_respond_ok() throws Exception {
		
		mvc.perform(
				put("/api/build")
				)
		.andExpect(
				status().isOk()
				);
	}
	
	@Test
	public void build_creates_smartgov_instance() throws Exception {
		
		mvc.perform(
				put("/api/build")
				);
		
		assertThat(
				SmartGovController.smartGov,
				notNullValue()
				);
	}
	
	@Test
	public void start_with_no_build_respond_bad_request() throws Exception {
		SmartGovController.smartGov = null;
		mvc.perform(
				put("/api/start")
				)
		.andExpect(
				status().isBadRequest()
				);
	}
	
	@Test
	public void start_for_a_given_number_of_ticks() throws Exception {
		mvc.perform(
				put("/api/build")
				);
		
		mvc.perform(
				put("/api/start?simulationDuration=10&tickDuration=1")
				);
		
		while(SmartGov.getRuntime().isRunning()) {
			TimeUnit.MICROSECONDS.sleep(10);
		}
		
		assertThat(
				SmartGov.getRuntime().getTickCount(),
				equalTo(10)
				);
	}
	
	@Test
	public void pause_and_resume_simulation() throws Exception {
		mvc.perform(
				put("/api/build")
				);

		
		
		EventTriggeredChecker checker = new EventTriggeredChecker();
		SmartGov.getRuntime().addSimulationStepListener(new EventHandler<SimulationStep>() {

			@Override
			public void handle(SimulationStep event) {
				if (event.getTick() == 5) {
					checker.triggered = true;
					try {
						mvc.perform(
							put("/api/pause")
							);
					} catch (Exception e1) {
						e1.printStackTrace();
					}
					
					try {
						TimeUnit.SECONDS.sleep(2);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					assertThat(
						SmartGov.getRuntime().isRunning(),
						equalTo(true)
						);
					
					assertThat(
						SmartGov.getRuntime().getTickCount(),
						equalTo(5)
						);
					
					try {
						mvc.perform(
							put("/api/resume")
							);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
			}
			
		});
		
		mvc.perform(
				put("/api/start?simulationDuration=100&tickDuration=1")
				);
		
		while(SmartGov.getRuntime().isRunning()) {
			TimeUnit.MICROSECONDS.sleep(100);
		}
		
		assertThat(
			checker.triggered,
			equalTo(true)
			);
		
		assertThat(
			SmartGov.getRuntime().getTickCount(),
			equalTo(100)
			);
	}
	
	private class EventTriggeredChecker {
		public boolean triggered = false;
	}
}
