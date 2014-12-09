package qora.transaction;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ntp.NTP;

import org.json.simple.JSONObject;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

import qora.account.Account;
import qora.account.PrivateKeyAccount;
import qora.account.PublicKeyAccount;
import qora.crypto.Base58;
import qora.crypto.Crypto;
import database.DBSet;

public class ArbitraryTransaction extends Transaction
{
	private static final int CREATOR_LENGTH = 32;
	private static final int SERVICE_LENGTH = 4;
	private static final int DATA_SIZE_LENGTH = 4;
	private static final int REFERENCE_LENGTH = 64;
	private static final int FEE_LENGTH = 8;
	private static final int SIGNATURE_LENGTH = 64;
	private static final int BASE_LENGTH = TIMESTAMP_LENGTH + REFERENCE_LENGTH + CREATOR_LENGTH + SERVICE_LENGTH + DATA_SIZE_LENGTH + FEE_LENGTH + SIGNATURE_LENGTH;
	
	private PublicKeyAccount creator;
	private int service;
	private byte[] data;
	
	
	public ArbitraryTransaction(PublicKeyAccount creator, int service, byte[] data, BigDecimal fee, long timestamp, byte[] reference, byte[] signature) {
		super(ARBITRARY_TRANSACTION, fee, timestamp, reference, signature);
		
		this.service = service;
		this.data = data;
		this.creator = creator;
	}
	
	//GETTERS/SETTERS
	
	public int getService()
	{
		return this.service;
	}
	
	public byte[] getData() 
	{
		return this.data;
	}
	
	//PARSE CONVERT
	
	public static Transaction Parse(byte[] data) throws Exception
	{	
		//CHECK IF WE MATCH BLOCK LENGTH
		if(data.length < BASE_LENGTH)
		{
			throw new Exception("Data does not match block length");
		}
		
		int position = 0;
		
		//READ TIMESTAMP
		byte[] timestampBytes = Arrays.copyOfRange(data, position, position + TIMESTAMP_LENGTH);
		long timestamp = Longs.fromByteArray(timestampBytes);	
		position += TIMESTAMP_LENGTH;
		
		//READ REFERENCE
		byte[] reference = Arrays.copyOfRange(data, position, position + REFERENCE_LENGTH);
		position += REFERENCE_LENGTH;
		
		//READ CREATOR
		byte[] creatorBytes = Arrays.copyOfRange(data, position, position + CREATOR_LENGTH);
		PublicKeyAccount creator = new PublicKeyAccount(creatorBytes);
		position += CREATOR_LENGTH;
		
		//READ SERVICE
		byte[] serviceBytes = Arrays.copyOfRange(data, position, position + SERVICE_LENGTH);
		int service = Ints.fromByteArray(serviceBytes);	
		position += SERVICE_LENGTH;
		
		//READ DATA SIZE
		byte[] dataSizeBytes = Arrays.copyOfRange(data, position, position + DATA_SIZE_LENGTH);
		int dataSize = Ints.fromByteArray(dataSizeBytes);	
		position += DATA_SIZE_LENGTH;
		
		//READ DATA
		byte[] arbitraryData = Arrays.copyOfRange(data, position, position + dataSize);
		position += dataSize;
		
		//READ FEE
		byte[] feeBytes = Arrays.copyOfRange(data, position, position + FEE_LENGTH);
		BigDecimal fee = new BigDecimal(new BigInteger(feeBytes), 8);
		position += FEE_LENGTH;		
		
		//READ SIGNATURE
		byte[] signatureBytes = Arrays.copyOfRange(data, position, position + SIGNATURE_LENGTH);
		
		return new ArbitraryTransaction(creator, service, arbitraryData, fee, timestamp, reference, signatureBytes);
	}	

	@SuppressWarnings("unchecked")
	@Override
	public JSONObject toJson() 
	{
		//GET BASE
		JSONObject transaction = this.getJsonBase();
								
		//ADD CREATOR/SERVICE/DATA
		transaction.put("creator", this.creator.getAddress());
		transaction.put("service", this.service);
		transaction.put("data", Base58.encode(this.data));
								
		return transaction;	
	}

	@Override
	public byte[] toBytes() 
	{
		byte[] data = new byte[0];
		
		//WRITE TYPE
		byte[] typeBytes = Ints.toByteArray(ARBITRARY_TRANSACTION);
		typeBytes = Bytes.ensureCapacity(typeBytes, TYPE_LENGTH, 0);
		data = Bytes.concat(data, typeBytes);
		
		//WRITE TIMESTAMP
		byte[] timestampBytes = Longs.toByteArray(this.timestamp);
		timestampBytes = Bytes.ensureCapacity(timestampBytes, TIMESTAMP_LENGTH, 0);
		data = Bytes.concat(data, timestampBytes);
		
		//WRITE REFERENCE
		data = Bytes.concat(data, this.reference);
		
		//WRITE CREATOR
		data = Bytes.concat(data, this.creator.getPublicKey());
		
		//WRITE SERVICE
		byte[] serviceBytes = Ints.toByteArray(this.service);
		data = Bytes.concat(data, serviceBytes);
		
		//WRITE DATA SIZE
		byte[] dataSizeBytes = Ints.toByteArray(this.data.length);
		data = Bytes.concat(data, dataSizeBytes);
		
		//WRITE DATA
		data = Bytes.concat(data, this.data);
		
		//WRITE FEE
		byte[] feeBytes = this.fee.unscaledValue().toByteArray();
		byte[] fill = new byte[FEE_LENGTH - feeBytes.length];
		feeBytes = Bytes.concat(fill, feeBytes);
		data = Bytes.concat(data, feeBytes);

		//SIGNATURE
		data = Bytes.concat(data, this.signature);
		
		return data;	
	}

	@Override
	public int getDataLength() 
	{
		return TYPE_LENGTH + BASE_LENGTH + this.data.length;
	}
	
	//VALIDATE

	@Override
	public boolean isSignatureValid() 
	{
		byte[] data = new byte[0];
		
		//WRITE TYPE
		byte[] typeBytes = Ints.toByteArray(ARBITRARY_TRANSACTION);
		typeBytes = Bytes.ensureCapacity(typeBytes, TYPE_LENGTH, 0);
		data = Bytes.concat(data, typeBytes);
		
		//WRITE TIMESTAMP
		byte[] timestampBytes = Longs.toByteArray(this.timestamp);
		timestampBytes = Bytes.ensureCapacity(timestampBytes, TIMESTAMP_LENGTH, 0);
		data = Bytes.concat(data, timestampBytes);
		
		//WRITE REFERENCE
		data = Bytes.concat(data, this.reference);
		
		//WRITE CREATOR
		data = Bytes.concat(data, this.creator.getPublicKey());
		
		//WRITE SERVICE
		byte[] serviceBytes = Ints.toByteArray(this.service);
		data = Bytes.concat(data, serviceBytes);
		
		//WRITE DATA SIZE
		byte[] dataSizeBytes = Ints.toByteArray(this.data.length);
		data = Bytes.concat(data, dataSizeBytes);
		
		//WRITE DATA
		data = Bytes.concat(data, this.data);
		
		//WRITE FEE
		byte[] feeBytes = this.fee.unscaledValue().toByteArray();
		byte[] fill = new byte[FEE_LENGTH - feeBytes.length];
		feeBytes = Bytes.concat(fill, feeBytes);
		data = Bytes.concat(data, feeBytes);
		
		return Crypto.getInstance().verify(this.creator.getPublicKey(), this.signature, data);
	}

	@Override
	public int isValid(DBSet db) 
	{
		//CHECK IF RELEASED
		if(NTP.getTime() < ARBITRARY_TRANSACTIONS_RELEASE)
		{
			return NOT_YET_RELEASED;
		}
		
		//CHECK DATA SIZE
		if(data.length > 4000 || data.length < 1)
		{
			return INVALID_DATA_LENGTH;
		}
		
		//CHECK IF CREATOR HAS ENOUGH MONEY
		if(this.creator.getBalance(1, db).compareTo(this.fee) == -1)
		{
			return NO_BALANCE;
		}
		
		//CHECK IF REFERENCE IS OKE
		if(!Arrays.equals(this.creator.getLastReference(db), this.reference))
		{
			return INVALID_REFERENCE;
		}
		
		//CHECK IF FEE IS POSITIVE
		if(this.fee.compareTo(BigDecimal.ZERO) <= 0)
		{
			return NEGATIVE_FEE;
		}
		
		return VALIDATE_OKE;
	}
	
	//PROCESS/ORPHAN

	@Override
	public void process(DBSet db) 
	{
		//UPDATE CREATOR
		this.creator.setConfirmedBalance(this.creator.getConfirmedBalance(db).subtract(this.fee), db);
		
		//UPDATE REFERENCE OF CREATOR
		this.creator.setLastReference(this.signature, db);
	}

	@Override
	public void orphan(DBSet db) 
	{
		//UPDATE CREATOR
		this.creator.setConfirmedBalance(this.creator.getConfirmedBalance(db).add(this.fee), db);
		
		//UPDATE REFERENCE OF CREATOR
		this.creator.setLastReference(this.reference, db);
	}

	@Override
	public Account getCreator() 
	{
		return this.creator;
	}

	@Override
	public List<Account> getInvolvedAccounts()
	{
		List<Account> accounts = new ArrayList<Account>();
		
		accounts.add(this.creator);
		
		return accounts;
	}

	@Override
	public boolean isInvolved(Account account) 
	{
		String address = account.getAddress();
		
		if(address.equals(this.creator.getAddress()))
		{
			return true;
		}
	
		return false;
	}

	@Override
	public BigDecimal getAmount(Account account) 
	{
		String address = account.getAddress();
		
		if(address.equals(this.creator.getAddress()))
		{
			return BigDecimal.ZERO.setScale(8).subtract(this.fee);
		}
		
		return BigDecimal.ZERO.setScale(8);
	}
	
	public static byte[] generateSignature(DBSet db, PrivateKeyAccount creator, int service, byte[] arbitraryData, BigDecimal fee, long timestamp) 
	{
		byte[] data = new byte[0];
		
		//WRITE TYPE
		byte[] typeBytes = Ints.toByteArray(ARBITRARY_TRANSACTION);
		typeBytes = Bytes.ensureCapacity(typeBytes, TYPE_LENGTH, 0);
		data = Bytes.concat(data, typeBytes);
		
		//WRITE TIMESTAMP
		byte[] timestampBytes = Longs.toByteArray(timestamp);
		timestampBytes = Bytes.ensureCapacity(timestampBytes, TIMESTAMP_LENGTH, 0);
		data = Bytes.concat(data, timestampBytes);
		
		//WRITE REFERENCE
		data = Bytes.concat(data, creator.getLastReference(db));
		
		//WRITE CREATOR
		data = Bytes.concat(data, creator.getPublicKey());
		
		//WRITE SERVICE
		byte[] serviceBytes = Ints.toByteArray(service);
		data = Bytes.concat(data, serviceBytes);
		
		//WRITE DATA SIZE
		byte[] dataSizeBytes = Ints.toByteArray(arbitraryData.length);
		data = Bytes.concat(data, dataSizeBytes);
		
		//WRITE DATA
		data = Bytes.concat(data, arbitraryData);
		
		//WRITE FEE
		byte[] feeBytes = fee.unscaledValue().toByteArray();
		byte[] fill = new byte[FEE_LENGTH - feeBytes.length];
		feeBytes = Bytes.concat(fill, feeBytes);
		data = Bytes.concat(data, feeBytes);
		
		return Crypto.getInstance().sign(creator, data);
	}
}
