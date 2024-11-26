package zzb.telegram.bot.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import zzb.telegram.bot.repository.entities.Chat;

public interface ChatRepo extends JpaRepository<Chat, String> {

	@Query("SELECT c.chatId FROM Chat c WHERE :value >= c.alertValue AND c.nextDrawReceived = false")
	public List<String> findAllByAlertValueNextDrawReceivedFalse(@Param("value") long value);

	@Query("SELECT c.chatId FROM Chat c WHERE :value >= c.alertValue AND c.nextDrawReceived = true")
	public List<String> findAllByAlertValueNextDrawReceivedTrue(@Param("value") long value);

	@Query("SELECT c.chatId FROM Chat c WHERE :value >= c.alertValue AND c.nextDrawReceived = :nextDrawReceived")
	public List<String> findAllByAlertValueNextDrawReceived(
			@Param("value") long value,
			@Param("nextDrawReceived") boolean nextDrawReceived);

	@Modifying
	@Transactional
	@Query("UPDATE Chat c SET c.nextDrawReceived = :nextDrawReceived where c.chatId IN :chatIds")
	public void updateChatsNextDrawReceived(
			@Param("nextDrawReceived") boolean nextDrawReceived,
			@Param("chatIds") List<String> chatIds);

	@Modifying
	@Transactional
	@Query("UPDATE Chat c SET c.nextDrawReceived = false")
	public void updateChatsNextDrawReceivedToFalse();

	@Query("SELECT c.chatId FROM Chat c")
	public List<String> findAllReturnChatId();

	@Modifying
	@Transactional
	@Query("DELETE FROM Chat c WHERE c.chatId = :chatId")
	public void deleteChat(String chatId);

}
