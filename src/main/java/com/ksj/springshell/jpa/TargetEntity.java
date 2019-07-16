package com.ksj.springshell.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "TARGET")
public class TargetEntity {

    @Id
    private Long id;

    @Column(length = 20, nullable = false, unique = true)
    private String ip;

    @Column(length = 5, nullable = false)
    private String port;

}
