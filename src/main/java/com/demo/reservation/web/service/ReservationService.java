package com.demo.reservation.web.service;

import com.demo.reservation.web.dao.ReservationDAO;
import com.demo.reservation.web.entity.Reservation;
import com.demo.reservation.web.entity.Room;
import com.demo.reservation.web.entity.User;
import com.demo.reservation.web.exception.ConflictException;
import com.demo.reservation.web.exception.NoContentException;
import com.demo.reservation.web.exception.NotFoundException;
import com.demo.reservation.web.pojo.request.ReservationCreateBody;
import com.demo.reservation.web.util.TimeUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class ReservationService {

    private ReservationDAO dao;
    private RoomService    roomService;
    private UserService    userService;

    public ReservationService(ReservationDAO dao, RoomService roomService, UserService userService) {

        this.dao = dao;
        this.roomService = roomService;
        this.userService = userService;
    }

    public Reservation findById(Long id) {

        return dao.findById(id).orElseThrow(() -> new NotFoundException(id, Reservation.class));
    }

    public List<Reservation> findAllByDay(LocalDate day) throws NoContentException {

        List<Reservation> result = dao.findAllByDay(day);
        if (result.isEmpty()) {
            throw new NoContentException("empty!");
        }

        return result;
    }

    public void create(ReservationCreateBody body) {

        create(body.getRoomId(), body.getUserId(), body.getDay(), body.getStartTime(), body.getEndTime(), body.getRepeatCount());
    }

    public void create(Long roomId, Long userId, LocalDate day, LocalTime startTime, LocalTime endTime, Integer repeatCount) {

        List<Integer> timeTableSequence = TimeUtils.getTimeTableSequence(startTime, endTime);

        for (Integer week = 0; week <= repeatCount; week++) {
            final LocalDate finalDay = day.plusWeeks(week);
            if (dao.hasConflict(roomId, finalDay, timeTableSequence)) {
                throw new ConflictException(roomId, Reservation.class);
            }
        }

        Room room = roomService.findById(roomId);
        User user = userService.findById(userId);
        dao.bulkInsert(timeTableSequence, repeatCount, day, room, user);

    }
}
