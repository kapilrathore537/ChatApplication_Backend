package com.chat.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chat.model.LastSeen;

public interface LastSeenRepository extends JpaRepository<LastSeen, String> {

}
