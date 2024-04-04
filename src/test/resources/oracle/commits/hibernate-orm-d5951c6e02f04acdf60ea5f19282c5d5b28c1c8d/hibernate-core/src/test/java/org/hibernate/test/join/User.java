/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.join;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.SecondaryTable;
import javax.persistence.SecondaryTables;

import org.hibernate.annotations.ColumnTransformer;

/**
 * @author Mike Dillon
 * @author Steve Ebersole
 */
@Entity
@DiscriminatorValue( "U" )
@SecondaryTables({
		@SecondaryTable(name = "t_user"),
		@SecondaryTable(name = "t_silly")
})
public class User extends Person {
	private String login;
	private Double passwordExpiryDays;
	private String silly;

	@Column(table = "t_user", name = "u_login")
	public String getLogin() {
		return login;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	@Column(table = "t_user", name = "pwd_expiry_weeks")
	@ColumnTransformer( read = "pwd_expiry_weeks * 7.0E0", write = "? / 7.0E0")
	public Double getPasswordExpiryDays() {
		return passwordExpiryDays;
	}

	public void setPasswordExpiryDays(Double passwordExpiryDays) {
		this.passwordExpiryDays = passwordExpiryDays;
	}

	@Column(table = "t_silly")
	public String getSilly() {
		return silly;
	}

	public void setSilly(String silly) {
		this.silly = silly;
	}
}
