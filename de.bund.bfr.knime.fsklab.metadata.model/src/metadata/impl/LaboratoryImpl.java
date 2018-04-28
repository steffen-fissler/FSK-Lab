/**
 */
package metadata.impl;

import java.util.Collection;

import metadata.Laboratory;
import metadata.MetadataPackage;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

import org.eclipse.emf.ecore.util.EDataTypeUniqueEList;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Laboratory</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link metadata.impl.LaboratoryImpl#getLaboratoryAccreditation <em>Laboratory Accreditation</em>}</li>
 *   <li>{@link metadata.impl.LaboratoryImpl#getLaboratoryName <em>Laboratory Name</em>}</li>
 *   <li>{@link metadata.impl.LaboratoryImpl#getLaboratoryCountry <em>Laboratory Country</em>}</li>
 * </ul>
 *
 * @generated
 */
public class LaboratoryImpl extends MinimalEObjectImpl.Container implements Laboratory {
	/**
	 * The cached value of the '{@link #getLaboratoryAccreditation() <em>Laboratory Accreditation</em>}' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getLaboratoryAccreditation()
	 * @generated
	 * @ordered
	 */
	protected EList<String> laboratoryAccreditation;

	/**
	 * The default value of the '{@link #getLaboratoryName() <em>Laboratory Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getLaboratoryName()
	 * @generated
	 * @ordered
	 */
	protected static final String LABORATORY_NAME_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getLaboratoryName() <em>Laboratory Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getLaboratoryName()
	 * @generated
	 * @ordered
	 */
	protected String laboratoryName = LABORATORY_NAME_EDEFAULT;

	/**
	 * The default value of the '{@link #getLaboratoryCountry() <em>Laboratory Country</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getLaboratoryCountry()
	 * @generated
	 * @ordered
	 */
	protected static final String LABORATORY_COUNTRY_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getLaboratoryCountry() <em>Laboratory Country</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getLaboratoryCountry()
	 * @generated
	 * @ordered
	 */
	protected String laboratoryCountry = LABORATORY_COUNTRY_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected LaboratoryImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return MetadataPackage.Literals.LABORATORY;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList<String> getLaboratoryAccreditation() {
		if (laboratoryAccreditation == null) {
			laboratoryAccreditation = new EDataTypeUniqueEList<String>(String.class, this, MetadataPackage.LABORATORY__LABORATORY_ACCREDITATION);
		}
		return laboratoryAccreditation;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getLaboratoryName() {
		return laboratoryName;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setLaboratoryName(String newLaboratoryName) {
		String oldLaboratoryName = laboratoryName;
		laboratoryName = newLaboratoryName;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MetadataPackage.LABORATORY__LABORATORY_NAME, oldLaboratoryName, laboratoryName));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getLaboratoryCountry() {
		return laboratoryCountry;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setLaboratoryCountry(String newLaboratoryCountry) {
		String oldLaboratoryCountry = laboratoryCountry;
		laboratoryCountry = newLaboratoryCountry;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MetadataPackage.LABORATORY__LABORATORY_COUNTRY, oldLaboratoryCountry, laboratoryCountry));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case MetadataPackage.LABORATORY__LABORATORY_ACCREDITATION:
				return getLaboratoryAccreditation();
			case MetadataPackage.LABORATORY__LABORATORY_NAME:
				return getLaboratoryName();
			case MetadataPackage.LABORATORY__LABORATORY_COUNTRY:
				return getLaboratoryCountry();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case MetadataPackage.LABORATORY__LABORATORY_ACCREDITATION:
				getLaboratoryAccreditation().clear();
				getLaboratoryAccreditation().addAll((Collection<? extends String>)newValue);
				return;
			case MetadataPackage.LABORATORY__LABORATORY_NAME:
				setLaboratoryName((String)newValue);
				return;
			case MetadataPackage.LABORATORY__LABORATORY_COUNTRY:
				setLaboratoryCountry((String)newValue);
				return;
		}
		super.eSet(featureID, newValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eUnset(int featureID) {
		switch (featureID) {
			case MetadataPackage.LABORATORY__LABORATORY_ACCREDITATION:
				getLaboratoryAccreditation().clear();
				return;
			case MetadataPackage.LABORATORY__LABORATORY_NAME:
				setLaboratoryName(LABORATORY_NAME_EDEFAULT);
				return;
			case MetadataPackage.LABORATORY__LABORATORY_COUNTRY:
				setLaboratoryCountry(LABORATORY_COUNTRY_EDEFAULT);
				return;
		}
		super.eUnset(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean eIsSet(int featureID) {
		switch (featureID) {
			case MetadataPackage.LABORATORY__LABORATORY_ACCREDITATION:
				return laboratoryAccreditation != null && !laboratoryAccreditation.isEmpty();
			case MetadataPackage.LABORATORY__LABORATORY_NAME:
				return LABORATORY_NAME_EDEFAULT == null ? laboratoryName != null : !LABORATORY_NAME_EDEFAULT.equals(laboratoryName);
			case MetadataPackage.LABORATORY__LABORATORY_COUNTRY:
				return LABORATORY_COUNTRY_EDEFAULT == null ? laboratoryCountry != null : !LABORATORY_COUNTRY_EDEFAULT.equals(laboratoryCountry);
		}
		return super.eIsSet(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String toString() {
		if (eIsProxy()) return super.toString();

		StringBuffer result = new StringBuffer(super.toString());
		result.append(" (laboratoryAccreditation: ");
		result.append(laboratoryAccreditation);
		result.append(", laboratoryName: ");
		result.append(laboratoryName);
		result.append(", laboratoryCountry: ");
		result.append(laboratoryCountry);
		result.append(')');
		return result.toString();
	}

} //LaboratoryImpl
